package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.entity.enums.BillingAuditType;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.entity.enums.TransactionType;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.TokenPackage;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.repository.plans.TokenPackageRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.logs.BillingAuditService;
import com.app.civify.service.notification.NotificationService;
import com.dodopayments.api.client.DodoPaymentsClient;
import com.dodopayments.api.core.JsonValue;
import com.dodopayments.api.models.checkoutsessions.CheckoutSessionCreateParams;
import com.dodopayments.api.models.checkoutsessions.CheckoutSessionRequest;
import com.dodopayments.api.models.checkoutsessions.CheckoutSessionResponse;
import com.dodopayments.api.models.checkoutsessions.CheckoutSessionStatus;
import com.dodopayments.api.models.checkoutsessions.ProductItemReq;
import com.dodopayments.api.models.payments.IntentStatus;
import com.dodopayments.api.models.payments.Payment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DodoPaymentGateway implements PaymentGateway {

    static final String METADATA_CIVIFY_REFERENCE = "civify_reference";

    private final Optional<DodoPaymentsClient> dodoClient;
    private final BillingTransactionRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final TokenPackageRepository tokenPackageRepository;
    private final ObjectMapper objectMapper;
    private final PaymentFulfillmentService fulfillmentService;
    private final NotificationService notificationService;
    private final BillingAuditService billingAuditService;

    @Value("${payment.gateway.dodo.enabled:false}")
    private boolean enabled;

    @Value("${payment.dodo.webhook-key:}")
    private String webhookKey;

    @Value("${payment.dodo.return-url:}")
    private String returnUrl;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getGatewayName() {
        return "dodo";
    }

    @Override
    public Long getPaymentMethod(PaymentMethods paymentMethods) {
        return 0L;
    }

    @Override
    @Transactional
    public PaymentCreationResult createPayment(Long userId, double amount, PaymentMethods method, TransactionType type, Map<String, Object> metadata) {
        if (!isEnabled()) {
            throw new RuntimeException("Dodo Payments gateway is not configured properly.");
        }
        if (dodoClient.isEmpty()) {
            throw new IllegalStateException("DodoPaymentsClient is disabled or not configured.");
        }

        try {
            log.info("Creating Dodo checkout for user {} amount {} USD type {}", userId, amount, type);

            String dodoProductId = resolveDodoProductId(type, metadata);
            String civifyReference = "civify_dodo_" + userId + "_" + System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().substring(0, 8);

            CheckoutSessionRequest.Metadata.Builder sessionMetadataBuilder =
                    CheckoutSessionRequest.Metadata.builder()
                            .putAdditionalProperty(METADATA_CIVIFY_REFERENCE, JsonValue.from(civifyReference));
            if (metadata != null) {
                if (metadata.containsKey("subscriptionId")) {
                    sessionMetadataBuilder.putAdditionalProperty(
                            "subscription_id", JsonValue.from(String.valueOf(metadata.get("subscriptionId"))));
                }
                if (metadata.containsKey("packageId")) {
                    sessionMetadataBuilder.putAdditionalProperty(
                            "package_id", JsonValue.from(String.valueOf(metadata.get("packageId"))));
                }
                if (metadata.containsKey("purchaseId")) {
                    sessionMetadataBuilder.putAdditionalProperty(
                            "purchase_id", JsonValue.from(String.valueOf(metadata.get("purchaseId"))));
                }
            }
            CheckoutSessionRequest.Metadata sessionMetadata = sessionMetadataBuilder.build();

            ProductItemReq productItem = ProductItemReq.builder()
                    .productId(dodoProductId)
                    .quantity(1)
                    .build();

            CheckoutSessionRequest sessionRequest = CheckoutSessionRequest.builder()
                    .addProductCart(productItem)
                    .returnUrl(buildReturnUrl())
                    .metadata(sessionMetadata)
                    .build();

            CheckoutSessionCreateParams params = CheckoutSessionCreateParams.builder()
                    .checkoutSessionRequest(sessionRequest)
                    .build();

            CheckoutSessionResponse response = dodoClient.get().checkoutSessions().create(params);
            String sessionId = response.sessionId();
            String checkoutUrl = response.checkoutUrl().orElse("");

            log.info("Dodo session {} created for civify ref {}", sessionId, civifyReference);

            BillingTransaction transaction = new BillingTransaction();
            transaction.setUserId(userId);
            transaction.setAmount(BigDecimal.valueOf(amount));
            transaction.setOrderId(civifyReference);
            transaction.setPaymentKey(sessionId);
            transaction.setStatus(PaymentStatus.PENDING);
            transaction.setType(type);
            transaction.setGateway(getGatewayName());
            transaction.setCurrency("USD");

            if (metadata != null && metadata.containsKey("subscriptionId")) {
                transaction.setSubscriptionId(parseLong(metadata.get("subscriptionId")));
            }

            Map<String, Object> txMeta = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            txMeta.put("paymentUrl", checkoutUrl);
            txMeta.put("dodoSessionId", sessionId);
            txMeta.put(METADATA_CIVIFY_REFERENCE, civifyReference);
            transaction.setMetadata(objectMapper.writeValueAsString(txMeta));

            paymentRepository.save(transaction);

            return PaymentCreationResult.builder()
                    .paymentKey(sessionId)
                    .paymentUrl(checkoutUrl)
                    .orderId(civifyReference)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Dodo checkout session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Dodo Payments checkout session", e);
        }
    }

    private String resolveDodoProductId(TransactionType type, Map<String, Object> metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata is required for Dodo Payments.");
        }

        if (type == TransactionType.TOKEN_TOPUP && metadata.containsKey("packageId")) {
            Long packageId = parseLong(metadata.get("packageId"));
            TokenPackage tokenPackage = tokenPackageRepository.findById(packageId)
                    .orElseThrow(() -> new IllegalArgumentException("Token package not found: " + packageId));
            String productId = tokenPackage.getDodoProductId();
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException(
                        "No Dodo product ID for token package: " + tokenPackage.getName()
                                + ". Set token_packages.dodo_product_id in the database.");
            }
            return productId;
        }

        Long subscriptionId = parseLong(metadata.get("subscriptionId"));
        if (subscriptionId == null) {
            throw new IllegalArgumentException(
                    "subscriptionId or packageId (TOKEN_TOPUP) is required for Dodo Payments.");
        }

        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        SubscriptionPlan plan = subscription.getPlan();
        if (plan == null) {
            throw new IllegalArgumentException("Subscription has no associated plan.");
        }

        String dodoProductId = plan.getDodoProductId();
        if (dodoProductId == null || dodoProductId.isBlank()) {
            throw new IllegalArgumentException(
                    "No Dodo product ID for plan: " + plan.getName()
                            + ". Set subscription_plans.dodo_product_id in the database.");
        }
        return dodoProductId;
    }

    @Override
    public boolean verifyPayment(String transactionId) {
        if (dodoClient.isEmpty() || transactionId == null || transactionId.isBlank()) {
            return false;
        }
        try {
            if (transactionId.startsWith("civify_")) {
                return paymentRepository.findByOrderId(transactionId)
                        .map(tx -> tx.getStatus() == PaymentStatus.COMPLETED)
                        .orElse(false);
            }
            if (transactionId.startsWith("cks_")) {
                CheckoutSessionStatus sessionStatus = dodoClient.get().checkoutSessions().retrieve(transactionId);
                IntentStatus status = sessionStatus.paymentStatus().orElse(null);
                return status == IntentStatus.SUCCEEDED;
            }
            if (transactionId.startsWith("pay_")) {
                Payment payment = dodoClient.get().payments().retrieve(transactionId);
                IntentStatus status = payment.status().orElse(null);
                return status == IntentStatus.SUCCEEDED;
            }
            return paymentRepository.findByOrderId(transactionId)
                    .or(() -> paymentRepository.findByPaymentKey(transactionId))
                    .map(tx -> tx.getStatus() == PaymentStatus.COMPLETED)
                    .orElse(false);
        } catch (Exception e) {
            log.error("Failed to verify Dodo payment {}: {}", transactionId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        log.info("Processing Dodo Payments webhook");

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            if (!verifyWebhookSignature(rawPayload, headers)) {
                result.setSuccess(false);
                result.setMessage("WEBHOOK_SIGNATURE_INVALID");
                result.setErrorCode("WEBHOOK_SIGNATURE_INVALID");
                return result;
            }

            JsonNode root = objectMapper.readTree(rawPayload);
            String type = root.has("type") ? root.get("type").asText() : "";
            JsonNode dataNode = root.get("data");

            if (dataNode == null) {
                result.setSuccess(false);
                result.setMessage("INVALID_WEBHOOK_PAYLOAD");
                return result;
            }

            String paymentId = dataNode.has("payment_id") ? dataNode.get("payment_id").asText() : "";
            String civifyReference = extractCivifyReference(dataNode);

            if (civifyReference == null || civifyReference.isBlank()) {
                log.error("Missing civify_reference in Dodo webhook for payment {}", paymentId);
                result.setSuccess(false);
                result.setMessage("INVALID_WEBHOOK_PAYLOAD");
                return result;
            }

            log.info("Dodo webhook {} civifyRef={} paymentId={}", type, civifyReference, paymentId);

            BillingTransaction transaction = paymentRepository.findByOrderIdWithLock(civifyReference)
                    .orElseThrow(() -> new RuntimeException("TRANSACTION_NOT_FOUND"));

            result.setTransactionId(paymentId);
            result.setOrderId(civifyReference);

            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                result.setSuccess(true);
                result.setMessage("ALREADY_PROCESSED");
                result.setAlreadyProcessed(true);
                logWebhookAudit(transaction, civifyReference, true, "ALREADY_PROCESSED", rawPayload);
                return result;
            }

            if ("payment.succeeded".equalsIgnoreCase(type)) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                mergeTransactionMetadata(transaction, paymentId, null);
                paymentRepository.save(transaction);

                result.setSuccess(true);
                result.setMessage("PAYMENT_COMPLETED");

                try {
                    notificationService.sendToUser(transaction.getUserId(), "Payment Successful",
                            "Your payment was successful.", com.app.civify.entity.enums.NotificationType.SYSTEM);
                } catch (Exception ex) {
                    log.error("Failed to send payment notification: {}", ex.getMessage());
                }

                fulfillmentService.fulfillPayment(civifyReference);
                logWebhookAudit(transaction, civifyReference, true, result.getMessage(), rawPayload);

            } else if ("payment.failed".equalsIgnoreCase(type)) {
                transaction.setStatus(PaymentStatus.FAILED);
                mergeTransactionMetadata(transaction, paymentId, null);
                paymentRepository.save(transaction);
                result.setSuccess(false);
                result.setMessage("PAYMENT_FAILED");
                logWebhookAudit(transaction, civifyReference, false, result.getMessage(), rawPayload);
            } else {
                result.setSuccess(false);
                result.setMessage("EVENT_IGNORED");
            }

            return result;

        } catch (Exception e) {
            log.error("Error processing Dodo callback: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleRedirect(Map<String, String> params) {
        log.info("Processing Dodo redirect params: {}", params);

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            String dodoPaymentId = params.get("payment_id");
            String status = params.get("status");

            if (dodoPaymentId == null || dodoPaymentId.isBlank()) {
                result.setSuccess(false);
                result.setMessage("INVALID_REDIRECT_PARAMS");
                return result;
            }

            String civifyReference = resolveCivifyReferenceFromPaymentId(dodoPaymentId);
            if (civifyReference == null || civifyReference.isBlank()) {
                log.error("Could not resolve civify_reference from Dodo payment {}", dodoPaymentId);
                result.setSuccess(false);
                result.setMessage("TRANSACTION_NOT_FOUND");
                return result;
            }

            BillingTransaction transaction = paymentRepository.findByOrderIdWithLock(civifyReference)
                    .orElseThrow(() -> new RuntimeException("TRANSACTION_NOT_FOUND"));

            result.setTransactionId(dodoPaymentId);
            result.setOrderId(civifyReference);

            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                result.setSuccess(true);
                result.setMessage("ALREADY_PROCESSED");
                result.setAlreadyProcessed(true);
                return result;
            }

            if (isRedirectSuccess(params)) {
                boolean verified = verifyPayment(dodoPaymentId);
                if (verified) {
                    transaction.setStatus(PaymentStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    mergeTransactionMetadata(transaction, dodoPaymentId, null);
                    paymentRepository.save(transaction);

                    result.setSuccess(true);
                    result.setMessage("PAYMENT_COMPLETED");
                    fulfillmentService.fulfillPayment(civifyReference);
                } else {
                    result.setSuccess(false);
                    result.setMessage("PAYMENT_PENDING");
                }
            } else {
                result.setSuccess(false);
                result.setMessage("PAYMENT_FAILED");
            }

            return result;
        } catch (Exception e) {
            log.error("Error processing Dodo redirect: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public boolean isRedirectSuccess(Map<String, String> params) {
        if (params == null) {
            return false;
        }
        String status = params.get("status");
        return "succeeded".equalsIgnoreCase(status) || "active".equalsIgnoreCase(status);
    }

    @Override
    public String resolveFulfillmentOrderId(String clientPaymentId) {
        if (clientPaymentId == null || clientPaymentId.isBlank()) {
            return clientPaymentId;
        }
        if (clientPaymentId.startsWith("civify_")) {
            return clientPaymentId;
        }
        if (clientPaymentId.startsWith("pay_")) {
            String ref = resolveCivifyReferenceFromPaymentId(clientPaymentId);
            return ref != null ? ref : clientPaymentId;
        }
        return paymentRepository.findByOrderId(clientPaymentId)
                .map(BillingTransaction::getOrderId)
                .orElseGet(() -> paymentRepository.findByPaymentKey(clientPaymentId)
                        .map(BillingTransaction::getOrderId)
                        .orElse(clientPaymentId));
    }

    @Override
    public String getPaymentUrl(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .map(tx -> {
                    try {
                        JsonNode meta = objectMapper.readTree(tx.getMetadata());
                        if (meta.has("paymentUrl")) {
                            return meta.get("paymentUrl").asText();
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse metadata paymentUrl: {}", e.getMessage());
                    }
                    return "";
                })
                .orElse("");
    }

    private String buildReturnUrl() {
        String base = returnUrl != null ? returnUrl.trim() : "/payment/success";
        if (!base.contains("gateway=")) {
            base += base.contains("?") ? "&gateway=dodo" : "?gateway=dodo";
        }
        return base;
    }

    private String extractCivifyReference(JsonNode dataNode) {
        if (dataNode.has("metadata") && dataNode.get("metadata").isObject()) {
            JsonNode meta = dataNode.get("metadata");
            if (meta.has(METADATA_CIVIFY_REFERENCE)) {
                return meta.get(METADATA_CIVIFY_REFERENCE).asText();
            }
        }
        String paymentId = dataNode.has("payment_id") ? dataNode.get("payment_id").asText(null) : null;
        if (paymentId != null && !paymentId.isBlank()) {
            return resolveCivifyReferenceFromPaymentId(paymentId);
        }
        return null;
    }

    private String resolveCivifyReferenceFromPaymentId(String dodoPaymentId) {
        if (dodoClient.isEmpty()) {
            return null;
        }
        try {
            Payment payment = dodoClient.get().payments().retrieve(dodoPaymentId);
            Payment.Metadata paymentMeta = payment.metadata();
            if (paymentMeta != null) {
                JsonValue refValue = paymentMeta._additionalProperties().get(METADATA_CIVIFY_REFERENCE);
                if (refValue != null) {
                    Optional<String> ref = refValue.asString();
                    if (ref.isPresent() && !ref.get().isBlank()) {
                        return ref.get();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load Dodo payment {} metadata: {}", dodoPaymentId, e.getMessage());
        }
        return paymentRepository.findByPaymentKey(dodoPaymentId)
                .map(BillingTransaction::getOrderId)
                .orElse(null);
    }

    private void mergeTransactionMetadata(BillingTransaction transaction, String dodoPaymentId, String dodoSessionId) {
        try {
            Map<String, Object> metadataMap = new HashMap<>();
            if (transaction.getMetadata() != null && !transaction.getMetadata().isBlank()) {
                metadataMap = new HashMap<>(objectMapper.readValue(transaction.getMetadata(), Map.class));
            }
            if (dodoPaymentId != null) {
                metadataMap.put("dodoPaymentId", dodoPaymentId);
            }
            if (dodoSessionId != null) {
                metadataMap.put("dodoSessionId", dodoSessionId);
            }
            transaction.setMetadata(objectMapper.writeValueAsString(metadataMap));
        } catch (Exception e) {
            log.warn("Failed to merge transaction metadata: {}", e.getMessage());
        }
    }

    private void logWebhookAudit(BillingTransaction transaction, String orderId, boolean success,
                                 String message, String rawPayload) {
        try {
            billingAuditService.logWebhook(
                    getGatewayName(),
                    transaction.getUserId(),
                    null,
                    transaction.getPaymentKey(),
                    orderId,
                    success ? "success" : "failed",
                    "callback",
                    message,
                    rawPayload,
                    null,
                    success ? BillingAuditType.WEBHOOK_SUCCESS : BillingAuditType.WEBHOOK_FAILED
            );
        } catch (Exception auditEx) {
            log.warn("Failed to log billing audit (non-critical): {}", auditEx.getMessage());
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean verifyWebhookSignature(String rawPayload, Map<String, String> headers) {
        String webhookId = headers.get("webhook-id");
        String webhookSignature = headers.get("webhook-signature");
        String webhookTimestamp = headers.get("webhook-timestamp");

        if (webhookId == null || webhookSignature == null || webhookTimestamp == null) {
            log.warn("Missing required webhook headers for signature verification");
            return false;
        }

        if (webhookKey == null || webhookKey.isBlank()) {
            log.warn("Dodo webhook key is not configured, skipping signature verification (NOT SECURE!)");
            return true;
        }

        try {
            String cleanKey = webhookKey;
            if (cleanKey.startsWith("whsec_")) {
                cleanKey = cleanKey.substring(6);
            }
            byte[] keyBytes = java.util.Base64.getDecoder().decode(cleanKey);

            String toSign = webhookId + "." + webhookTimestamp + "." + rawPayload;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey =
                    new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(toSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            String[] signatures = webhookSignature.split(" ");
            for (String sig : signatures) {
                if (sig.startsWith("v1,")) {
                    String rawSig = sig.substring(3);
                    byte[] rawSigBytes = java.util.Base64.getDecoder().decode(rawSig);
                    if (java.security.MessageDigest.isEqual(signatureBytes, rawSigBytes)) {
                        return true;
                    }
                }
            }
            log.warn("Invalid webhook signature provided");
            return false;
        } catch (Exception e) {
            log.error("Failed to verify webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
}
