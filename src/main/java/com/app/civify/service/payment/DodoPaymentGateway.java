package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.entity.enums.TransactionType;
import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.notification.NotificationService;
import com.dodopayments.api.client.DodoPaymentsClient;
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
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DodoPaymentGateway implements PaymentGateway {
    private final Optional<DodoPaymentsClient> dodoClient;
    private final BillingTransactionRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final PaymentFulfillmentService fulfillmentService;
    private final NotificationService notificationService;

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
        return 0L; // Dodo handles payment method selection on their hosted page
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
            log.info("Creating Dodo Payments checkout session for user {} with amount {} USD", userId, amount);

            Long subscriptionId = null;
            if (metadata != null && metadata.containsKey("subscriptionId")) {
                Object subIdObj = metadata.get("subscriptionId");
                if (subIdObj instanceof Number) {
                    subscriptionId = ((Number) subIdObj).longValue();
                } else if (subIdObj instanceof String) {
                    subscriptionId = Long.parseLong((String) subIdObj);
                }
            }

            if (subscriptionId == null) {
                throw new IllegalArgumentException("Subscription ID is required for Dodo Payments.");
            }

            final Long finalSubscriptionId = subscriptionId;
            UserSubscription subscription = subscriptionRepository.findById(finalSubscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscription not found for ID: " + finalSubscriptionId));

            SubscriptionPlan plan = subscription.getPlan();
            if (plan == null) {
                throw new IllegalArgumentException("Subscription has no associated plan.");
            }

            String dodoProductId = plan.getDodoProductId();
            if (dodoProductId == null || dodoProductId.isBlank()) {
                throw new IllegalArgumentException("No Dodo Product ID mapped for plan: " + plan.getName());
            }

            log.info("Resolved Dodo Product ID: {} for plan: {}", dodoProductId, plan.getName());

            // Build Checkout Session Request
            ProductItemReq productItem = ProductItemReq.builder()
                    .productId(dodoProductId)
                    .quantity(1)
                    .build();

            // Include query params in returnUrl so redirect handling can determine success
            String targetReturnUrl = returnUrl;
            if (targetReturnUrl.contains("?")) {
                targetReturnUrl += "&payment_id={CHECKOUT_SESSION_ID}&status=succeeded";
            } else {
                targetReturnUrl += "?payment_id={CHECKOUT_SESSION_ID}&status=succeeded";
            }

            CheckoutSessionRequest sessionRequest = CheckoutSessionRequest.builder()
                    .addProductCart(productItem)
                    .returnUrl(targetReturnUrl)
                    .build();

            CheckoutSessionCreateParams params = CheckoutSessionCreateParams.builder()
                    .checkoutSessionRequest(sessionRequest)
                    .build();

            CheckoutSessionResponse response = dodoClient.get().checkoutSessions().create(params);
            String sessionId = response.sessionId();
            String checkoutUrl = response.checkoutUrl().orElse("");

            log.info("Dodo Checkout Session created. Session ID: {}, URL: {}", sessionId, checkoutUrl);

            // Save pending billing transaction
            BillingTransaction transaction = new BillingTransaction();
            transaction.setUserId(userId);
            transaction.setAmount(BigDecimal.valueOf(amount));
            transaction.setOrderId(sessionId);
            transaction.setPaymentKey(sessionId);
            transaction.setStatus(PaymentStatus.PENDING);
            transaction.setType(type);
            transaction.setGateway(getGatewayName());
            transaction.setCurrency("USD");
            transaction.setSubscriptionId(subscriptionId);

            Map<String, Object> mutableMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            mutableMetadata.put("paymentUrl", checkoutUrl);
            transaction.setMetadata(objectMapper.writeValueAsString(mutableMetadata));

            paymentRepository.save(transaction);

            return PaymentCreationResult.builder()
                    .paymentKey(sessionId)
                    .paymentUrl(checkoutUrl)
                    .orderId(sessionId)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Dodo Payments checkout session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Dodo Payments checkout session", e);
        }
    }

    @Override
    public boolean verifyPayment(String transactionId) {
        if (dodoClient.isEmpty()) {
            return false;
        }
        try {
            if (transactionId.startsWith("dos_")) {
                CheckoutSessionStatus sessionStatus = dodoClient.get().checkoutSessions().retrieve(transactionId);
                IntentStatus status = sessionStatus.paymentStatus().orElse(null);
                return status == IntentStatus.SUCCEEDED;
            } else {
                Payment payment = dodoClient.get().payments().retrieve(transactionId);
                IntentStatus status = payment.status().orElse(null);
                return status == IntentStatus.SUCCEEDED;
            }
        } catch (Exception e) {
            log.error("Failed to verify Dodo payment {}: {}", transactionId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        log.info("Processing Dodo Payments webhook callback");

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            // Step 1: Verify webhook signature
            if (!verifyWebhookSignature(rawPayload, headers)) {
                log.error("🔒 Dodo Payments signature verification FAILED - Potential security threat");
                result.setSuccess(false);
                result.setMessage("WEBHOOK_SIGNATURE_INVALID");
                result.setErrorCode("WEBHOOK_SIGNATURE_INVALID");
                return result;
            }
            log.debug("✅ Dodo signature verified");

            // Step 2: Parse JSON payload
            JsonNode root = objectMapper.readTree(rawPayload);
            String type = root.has("type") ? root.get("type").asText() : "";
            JsonNode dataNode = root.get("data");

            if (dataNode == null) {
                log.error("❌ Missing 'data' block in Dodo webhook payload");
                result.setSuccess(false);
                result.setMessage("INVALID_WEBHOOK_PAYLOAD");
                result.setErrorCode("INVALID_WEBHOOK_PAYLOAD");
                return result;
            }

            String checkoutSessionId = dataNode.has("checkout_session_id") ? dataNode.get("checkout_session_id").asText() : "";
            String paymentId = dataNode.has("payment_id") ? dataNode.get("payment_id").asText() : "";

            if (checkoutSessionId == null || checkoutSessionId.isBlank()) {
                log.error("❌ Missing checkout_session_id in Dodo webhook");
                result.setSuccess(false);
                result.setMessage("INVALID_WEBHOOK_PAYLOAD");
                result.setErrorCode("INVALID_WEBHOOK_PAYLOAD");
                return result;
            }

            log.info("Processing Dodo event {} for checkoutSessionId: {} paymentId: {}", type, checkoutSessionId, paymentId);

            // Step 3: Find transaction with PESSIMISTIC LOCK (prevents race conditions)
            BillingTransaction transaction = paymentRepository.findByOrderIdWithLock(checkoutSessionId)
                    .orElseThrow(() -> {
                        log.error("Transaction not found for Dodo checkout session: {}", checkoutSessionId);
                        return new RuntimeException("TRANSACTION_NOT_FOUND");
                    });

            result.setTransactionId(paymentId);
            result.setOrderId(checkoutSessionId);

            // Step 4: Idempotency check
            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                log.info("⚠️ Idempotent webhook detected - Dodo Checkout Session {} already COMPLETED", checkoutSessionId);
                result.setSuccess(true);
                result.setMessage("ALREADY_PROCESSED");
                result.setAlreadyProcessed(true);
                return result;
            }

            PaymentStatus previousStatus = transaction.getStatus();

            if ("payment.succeeded".equalsIgnoreCase(type)) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());

                Map<String, Object> metadataMap = new HashMap<>();
                if (transaction.getMetadata() != null && !transaction.getMetadata().isBlank()) {
                    try {
                        metadataMap = new HashMap<>(objectMapper.readValue(transaction.getMetadata(), Map.class));
                    } catch (Exception e) {
                        log.warn("Failed to parse existing metadata JSON: {}", e.getMessage());
                    }
                }
                metadataMap.put("dodoPaymentId", paymentId);
                transaction.setMetadata(objectMapper.writeValueAsString(metadataMap));

                paymentRepository.save(transaction);
                log.info("💾 Dodo payment COMPLETED. CheckoutSession: {} PaymentId: {}", checkoutSessionId, paymentId);

                result.setSuccess(true);
                result.setMessage("PAYMENT_COMPLETED");

                try {
                    notificationService.sendToUser(transaction.getUserId(), "Payment Successful",
                            "Your payment was successful and your plan is active.", com.app.civify.entity.enums.NotificationType.SYSTEM);
                } catch (Exception ex) {
                    log.error("Failed to send payment success notification: {}", ex.getMessage());
                }

                // Trigger fulfillment!
                fulfillmentService.fulfillPayment(checkoutSessionId);

            } else if ("payment.failed".equalsIgnoreCase(type)) {
                transaction.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(transaction);
                log.warn("❌ Dodo payment FAILED. CheckoutSession: {} PaymentId: {}", checkoutSessionId, paymentId);

                result.setSuccess(false);
                result.setMessage("PAYMENT_FAILED");
            } else {
                log.info("ℹ️ Ignored Dodo webhook event type: {}", type);
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
        log.info("Processing Dodo Payments redirect callback with params: {}", params);

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            String checkoutSessionId = params.get("payment_id");
            String status = params.get("status");

            if (checkoutSessionId == null || checkoutSessionId.isBlank()) {
                log.error("❌ Missing payment_id (checkout session ID) in Dodo redirect");
                result.setSuccess(false);
                result.setMessage("INVALID_REDIRECT_PARAMS");
                return result;
            }

            BillingTransaction transaction = paymentRepository.findByOrderIdWithLock(checkoutSessionId)
                    .orElseThrow(() -> new RuntimeException("TRANSACTION_NOT_FOUND"));

            result.setTransactionId(checkoutSessionId);
            result.setOrderId(checkoutSessionId);

            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                result.setSuccess(true);
                result.setMessage("ALREADY_PROCESSED");
                result.setAlreadyProcessed(true);
                return result;
            }

            if ("succeeded".equalsIgnoreCase(status)) {
                boolean verified = verifyPayment(checkoutSessionId);
                if (verified) {
                    transaction.setStatus(PaymentStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    paymentRepository.save(transaction);

                    log.info("✅ Dodo redirect verified successfully. Fulfilling payment: {}", checkoutSessionId);
                    result.setSuccess(true);
                    result.setMessage("PAYMENT_COMPLETED");

                    fulfillmentService.fulfillPayment(checkoutSessionId);
                } else {
                    log.warn("⚠️ Dodo redirect indicates success but API verification returned not succeeded yet.");
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
        return params != null && "succeeded".equalsIgnoreCase(params.get("status"));
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
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
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
