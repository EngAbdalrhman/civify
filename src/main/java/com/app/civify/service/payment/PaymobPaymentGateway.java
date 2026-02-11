package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.payment.paymob.PaymobAuthResponse;
import com.app.civify.dto.payment.paymob.PaymobOrderResponse;
import com.app.civify.dto.payment.paymob.PaymobPaymentKeyResponse;
import com.app.civify.entity.enums.BillingAuditType;
import com.app.civify.entity.plans.PaymentTransaction;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.plans.PaymentRepository;
import com.app.civify.service.logs.BillingAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Complete Paymob payment gateway implementation with full cycle support
 *
 * Payment Flow:
 * 1. Authenticate with Paymob API to get auth token
 * 2. Create an order with the payment amount
 * 3. Generate a payment key for the order
 * 4. User completes payment via Paymob iframe/widget
 * 5. Paymob sends webhook callback with payment status
 * 6. System verifies and activates subscription
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymobPaymentGateway implements PaymentGateway {

    private final PaymobClient paymobClient;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final BillingAuditService billingAuditService;

    @Lazy
    @Autowired
    private PaymentFulfillmentService fulfillmentService;

    @Value("${payment.paymob.api-key}")
    private String apiKey;

    @Value("${payment.paymob.iframe-id:}")
    private String iframeId;

    @Value("${payment.paymob.hmac-secret:}")
    private String hmacSecret;

    @Value("${payment.paymob.integration-id.card:0}")
    private Long cardIntegrationId;

    @Value("${payment.paymob.integration-id.wallet:0}")
    private Long walletIntegrationId;
    
    @Value("${payment.gateway.paymob.enabled:false}")
    private boolean enabled;

    // V2 Configuration
    @Value("${payment.paymob.secret-key:}")
    private String secretKey;

    @Value("${payment.paymob.public-key:}")
    private String publicKey;

    @Value("${payment.paymob.use-v2:false}")
    private boolean useV2;
    
    // Callback URLs (should be configured in application.properties or dynamic)
    @Value("${app.backend.base-url:http://localhost:8077}")
    private String backendUrl;
    
    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String getGatewayName() {
        return "paymob";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @Transactional
    public PaymentCreationResult createPayment(Long userId, double amount, PaymentMethods method, Map<String, Object> metadata) {
        if (!isEnabled()) {
            throw new RuntimeException("Paymob gateway is not configured properly. Please check api-key and integration-id in application.properties");
        }

        if (useV2) {
            return createPaymentV2(userId, amount, method, metadata);
        } else {
            return createPaymentLegacy(userId, amount, method, metadata);
        }
    }

    private PaymentCreationResult createPaymentV2(Long userId, double amount, PaymentMethods method, Map<String, Object> metadata) {
        try {
            log.info("Creating Paymob V2 payment (Intention) for user {} with amount {} EGP", userId, amount);

            // Null safety for metadata
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            Long integrationId = getPaymentMethod(method);
            int amountCents = (int) (amount * 100);

            // Construct Payload using ObjectMapper
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("amount", amountCents);
            payload.put("currency", "EGP");
            payload.putArray("payment_methods").add(integrationId);

            // Items (Required by V2)
            ArrayNode items = payload.putArray("items");
            ObjectNode item = items.addObject();
            item.put("name", (String) metadata.getOrDefault("planName", "Subscription"));
            item.put("amount", amountCents);
            item.put("description", "Civify Subscription");
            item.put("quantity", 1);

            // Billing Data
            ObjectNode billingData = payload.putObject("billing_data");
            billingData.put("first_name", (String) metadata.getOrDefault("firstName", "Customer"));
            billingData.put("last_name", (String) metadata.getOrDefault("lastName", "User"));
            billingData.put("email", (String) metadata.getOrDefault("email", "customer@example.com"));
            billingData.put("phone_number", (String) metadata.getOrDefault("phone", "+201000000000"));
            
            // Mandatory dummy fields for billing data
            billingData.put("apartment", "NA");
            billingData.put("floor", "NA");
            billingData.put("street", "NA");
            billingData.put("building", "NA");
            billingData.put("city", "NA");
            billingData.put("country", "NA");
            billingData.put("state", "NA");

            // Special Reference
            payload.put("special_reference", "civify_" + userId + "_" + System.currentTimeMillis());

            // Callbacks - CORRECTED PATH: /apis/v1/payments/webhook/{gateway}
            // Note: backendUrl usually does not include context path /apis, so we include it here.
            // If backendUrl includes /apis, user needs to adjust. Assuming standard base URL (e.g., https://api.civify.com)
            // The controller path is /v1/payments/webhook/paymob, context path is /apis.
            payload.put("notification_url", backendUrl + "/apis/v1/payments/webhook/paymob");
            payload.put("redirection_url", frontendUrl + "/payment/success?gateway=paymob");

            // Call V2 Endpoint
            String response = paymobClient.createIntention("Token " + secretKey, payload.toString());
            log.debug("Paymob V2 Intention Response: {}", response);

            JsonNode root = objectMapper.readTree(response);
            
            // Extract keys
            String clientSecret = root.get("client_secret").asText();
            String intentionId = root.get("id").asText();
            
            // Try to get order ID if available, otherwise use intention ID
            String orderId = intentionId; 
            if (root.has("order") && root.get("order").isNumber()) {
                orderId = String.valueOf(root.get("order").asLong());
            } else if (root.has("order_id")) {
                orderId = root.get("order_id").asText();
            }

            // Construct Payment URL (Unified Checkout)
            String paymentUrl = String.format("https://accept.paymob.com/unifiedcheckout/?publicKey=%s&clientSecret=%s",
                    publicKey, clientSecret);

            // Save Transaction
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setUserId(userId);
            transaction.setAmount(BigDecimal.valueOf(amount));
            transaction.setOrderId(orderId); 
            transaction.setPaymentKey(clientSecret); 
            transaction.setStatus(PaymentStatus.PENDING);
            transaction.setGateway(getGatewayName());
            transaction.setCurrency("EGP");
            
            // Store Intention ID in metadata for troubleshooting & verification
            Map<String, Object> txMetadata = new HashMap<>(metadata);
            txMetadata.put("intention_id", intentionId);
            txMetadata.put("is_v2", true);
            transaction.setMetadata(objectMapper.writeValueAsString(txMetadata));

            paymentRepository.save(transaction);
            
            log.info("Paymob V2 payment created. Intention: {}, Order: {}", intentionId, orderId);

            return PaymentCreationResult.builder()
                    .paymentKey(clientSecret)
                    .paymentUrl(paymentUrl)
                    .orderId(orderId)
                    .build();

        } catch (Exception e) {
            log.error("Error creating Paymob V2 payment for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create Paymob V2 payment: " + e.getMessage(), e);
        }
    }

    private PaymentCreationResult createPaymentLegacy(Long userId, double amount, PaymentMethods method, Map<String, Object> metadata) {
        try {
            log.info("Creating Paymob payment (Legacy) for user {} with amount {} EGP", userId, amount);

            // Step 1: Authenticate
            String authToken = authenticate();
            log.debug("Paymob authentication successful");

            // Step 2: Create order
            String orderId = createOrder(authToken, amount);
            log.debug("Paymob order created with ID: {}", orderId);

            // Step 3: Create payment key
            // Use the specific integration ID for the selected method
            Long integrationId = getPaymentMethod(method);
            String paymentKey = createPaymentKey(authToken, orderId, amount, integrationId.toString(), metadata);
            log.debug("Paymob payment key generated");

            // Step 4: Save transaction record
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setUserId(userId);
            transaction.setAmount(BigDecimal.valueOf(amount));
            transaction.setOrderId(orderId);
            transaction.setPaymentKey(paymentKey);
            transaction.setStatus(PaymentStatus.PENDING);
            transaction.setGateway(getGatewayName());
            transaction.setCurrency("EGP");

            // Store metadata if provided (e.g., subscription info)
            if (metadata != null && !metadata.isEmpty()) {
                try {
                    transaction.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize metadata: {}", e.getMessage());
                }
            }

            paymentRepository.save(transaction);

            log.info("Paymob payment created successfully for user {} with order ID {} and payment key {}",
                    userId, orderId, paymentKey);

            // Paymob uses iframe, so we can construct the URL here or let frontend do it.
            // To be consistent with Fawaterak, we should return the URL.
            String paymentUrl;
            
            // For WALLET, we need to make an additional request to process the payment
            if (method == PaymentMethods.WALLET) {
                String phone = "";
                if (metadata != null) {
                    phone = (String) metadata.getOrDefault("phone", "");
                }
                
                if (phone.isEmpty()) {
                    throw new RuntimeException("Phone number is required for wallet payments");
                }
                paymentUrl = processWalletPayment(paymentKey, phone);
                log.info("Paymob Wallet payment initiated. Redirect URL: {}", paymentUrl);
            } else {
                paymentUrl = getPaymentUrl(paymentKey);
            }

            return PaymentCreationResult.builder()
                    .paymentKey(paymentKey)
                    .paymentUrl(paymentUrl)
                    .orderId(orderId)
                    .build();

        } catch (Exception e) {
            log.error("Error creating Paymob payment for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create Paymob payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean verifyPayment(String paymentKey) {
        log.info("Verifying Paymob payment with key/order: {}", paymentKey);

        // Try orderId first (frontend now sends orderId from redirect params)
        PaymentTransaction transaction = paymentRepository.findByOrderId(paymentKey)
                .orElseGet(() -> paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found for key: " + paymentKey)));

        // If already completed (e.g. by webhook), return success immediately
        if (transaction.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment already completed for order {} (likely via webhook)", transaction.getOrderId());
            return true;
        }

        // Check compatibility: Is this a V2 transaction?
        boolean isV2 = isV2Transaction(transaction);
        boolean isSuccessful = false;

        if (isV2) {
             // V2 Verification: Use getIntention with Secret Key
             isSuccessful = verifyPaymentV2(transaction);
        } else {
             // Legacy Verification: Use getOrder with Auth Token
             isSuccessful = verifyPaymentLegacy(transaction);
        }

        if (isSuccessful) {
            transaction.setStatus(PaymentStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            log.info("Paymob payment verified successfully for user {}", transaction.getUserId());
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            log.warn("Paymob payment verification failed for user {}", transaction.getUserId());
        }

        paymentRepository.save(transaction);
        return isSuccessful;
    }

    private boolean isV2Transaction(PaymentTransaction transaction) {
        if (transaction.getMetadata() == null) return false;
        try {
            JsonNode meta = objectMapper.readTree(transaction.getMetadata());
            return meta.has("is_v2") && meta.get("is_v2").asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyPaymentV2(PaymentTransaction transaction) {
        try {
            // In V2, we usually rely on the intention ID. 
            // We stored intention_id in metadata, but also orderId might be the intention ID.
            String intentionId = transaction.getOrderId();
            
            // Try to get intention_id from metadata if present
            try {
                JsonNode meta = objectMapper.readTree(transaction.getMetadata());
                if (meta.has("intention_id")) {
                    intentionId = meta.get("intention_id").asText();
                }
            } catch (Exception e) {
                // ignore
            }

            log.debug("Verifying V2 Intention: {}", intentionId);
            String response = paymobClient.getIntention("Token " + secretKey, intentionId);
            
            // Parse Intention Response
            JsonNode root = objectMapper.readTree(response);
            
            // Check status. Usually check for "success" or payment method status?
            // Intention response structure needs checking. 
            // Assuming simplified check: Look for a successful payment method object or status.
            // But usually, we check the underlying Order or just the Intention status?
            // For now, if getIntention returns 200 OK and we have it, we check if it has 'payment_keys' which have transactions?
            // Actually, best way is to check the attached order's payment status if available.
            // But V2 intention response contains "next_action" or "payment_methods".
            
            // Fallback: If we got the intention successfully, and it has an order ID, we can check that order?
            // But getIntention logic is sufficient to prove existence. Proof of *payment* requires checking the payment transaction inside.
            
            // NOTE: Paymob V2 verification is best done via Webhook. Manual verification is tricky without looking at specific payment attempts.
            // Let's assume if we can fetch it, it exists. But we need to know if it's PAID.
            // We will return false here unless we can PROVE it's paid. 
            // Since we lack clear "is_paid" field in Intention without deep traversal, 
            // we will rely on Webhook primarily.
            // However, to support standard flow, we'll return false and log.
            log.warn("Manual verification for V2 not fully implemented (relying on Webhook). Returning false.");
            return false;
            
        } catch (Exception e) {
            log.error("V2 verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyPaymentLegacy(PaymentTransaction transaction) {
        // Authenticate and check payment status from Paymob Order (Acceptance API)
        // Let infrastructure exceptions (API errors, timeouts) propagate
        // so the controller can return 503 instead of a misleading "payment failed"
        String authToken = authenticate();
        String orderResponse = paymobClient.getOrder("Bearer " + authToken, transaction.getOrderId());

        // Parse response to check if payment is successful via Order transactions
        return parseOrderStatus(orderResponse);
    }

    /**
     * Parse order status from Paymob Order response (Acceptance API)
     */
    private boolean parseOrderStatus(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // Check paid_amount_cents vs amount_cents
            if (root.has("paid_amount_cents") && root.has("amount_cents")) {
                long paid = root.get("paid_amount_cents").asLong();
                long amount = root.get("amount_cents").asLong();
                // Check if fully paid
                if (amount > 0 && paid >= amount) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to parse order status: {}", e.getMessage());
            return false;
        }
    }


    @Override
    @Transactional
    public PaymentCallbackResult handleRedirect(Map<String, String> params) {
        log.info("Handling Paymob redirect callback");
        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            // Verify HMAC using params directly
            String hmac = params.get("hmac");
            if (hmac == null || !verifySignatureFromParams(params, hmac)) {
                log.warn("Paymob redirect failed HMAC verification");
                result.setSuccess(false);
                result.setMessage("Invalid signature");
                return result;
            }

            String orderId = params.get("order");
            result.setOrderId(orderId);
            result.setTransactionId(params.get("id"));

            boolean isSuccess = Boolean.parseBoolean(params.getOrDefault("success", "false"));

            // Find transaction
            PaymentTransaction transaction = paymentRepository.findByOrderId(orderId)
                     .orElseThrow(() -> new RuntimeException("Payment transaction not found for order: " + orderId));
            
            if (isSuccess) {
                if (transaction.getStatus() != PaymentStatus.COMPLETED) {
                    transaction.setStatus(PaymentStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    paymentRepository.save(transaction);
                    log.info("✅ Paymob redirect confirmed SUCCESS for order {}", orderId);
                    
                    // Fulfill the payment
                    fulfillmentService.fulfillPayment(orderId);
                }
                result.setSuccess(true);
                result.setMessage("Payment verified successfully");
            } else {
                if (transaction.getStatus() != PaymentStatus.FAILED) {
                    transaction.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(transaction);
                    log.warn("❌ Paymob redirect confirmed FAILURE for order {}", orderId);
                }
                result.setSuccess(false);
                result.setMessage("Payment failed");
            }
            
            // Log billing audit - convert params to JSON for proper storage (non-critical)
            try {
                String payloadJson = objectMapper.writeValueAsString(params);
                billingAuditService.logWebhook(
                    getGatewayName(),
                    transaction.getUserId(),
                    null,
                    transaction.getPaymentKey(),
                    orderId,
                    isSuccess ? "success" : "failed",
                    "redirect",
                    result.getMessage(),
                    payloadJson,
                    null,
                    isSuccess ? BillingAuditType.WEBHOOK_SUCCESS : BillingAuditType.WEBHOOK_FAILED
                );
            } catch (Exception auditEx) {
                log.warn("Failed to log billing audit for redirect (non-critical): {}", auditEx.getMessage());
            }
            
            return result;

        } catch (Exception e) {
            log.error("Error handling Paymob redirect: {}", e.getMessage());
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
            return result;
        }
    }

    @Override
    public boolean isRedirectSuccess(Map<String, String> params) {
        if (params == null) return false;
        return Boolean.parseBoolean(params.getOrDefault("success", "false"));
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            log.info("Handling Paymob payment callback");

            JsonNode root = objectMapper.readTree(rawPayload);
            String providedHmac = headers.getOrDefault("hmac", headers.getOrDefault("HMAC", null));
            
            // If HMAC is missing from headers, check if it's in the query string (controller needs to pass it)
            // But since the current controller implementation passes headers, we'll assume it's there or fail.
            // Note: The POST webhook puts HMAC in query params usually.
            // If providedHmac is null, we can try to extract from rawPayload if it mysteriously exists there (unlikely).
            
            // The JSON from Paymob POST callback is usually { "obj": { ... }, "type": "TRANSACTION" ... }
            JsonNode dataNode = root;
            if (root.has("obj")) {
                dataNode = root.get("obj");
            }

            if (providedHmac != null && !verifySignatureFromJson(dataNode, providedHmac)) {
                 log.warn("Paymob callback failed HMAC verification");
                 result.setSuccess(false);
                 result.setMessage("Invalid signature");
                 return result;
            }
            // If providedHmac is null, we log a warning but proceed or fail? 
            // In strict mode we should fail. Let's fail if it is null to be safe.
            if (providedHmac == null) {
                 log.warn("Paymob callback missing HMAC signature");
                 result.setSuccess(false);
                 result.setMessage("Missing signature");
                 return result;
            }

            log.debug("Callback data: {}", dataNode);

            // Extract order ID from callback
            String orderId = extractOrderId(dataNode.get("order"));

            if (orderId == null || orderId.isEmpty()) {
                log.warn("Order ID missing in callback data");
                result.setSuccess(false);
                result.setMessage("Order ID missing");
                return result;
            }
            
            result.setOrderId(orderId);
            result.setTransactionId(extractOrderId(dataNode.get("id")));

            PaymentTransaction transaction = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found for order: " + orderId));

            // Idempotency: if already completed, short-circuit
            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                log.info("Paymob callback idempotent hit for order {}, already COMPLETED", orderId);
                result.setSuccess(true);
                result.setMessage("Already processed");
                return result;
            }

            // Parse callback success status
            boolean isSuccessful = parseCallbackSuccess(dataNode);

            if (isSuccessful) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("✅ Paymob payment callback processed successfully for user {} - Order: {}",
                        transaction.getUserId(), orderId);
                result.setSuccess(true);
                result.setMessage("Payment completed");
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                log.warn("❌ Paymob payment callback indicates failure for user {} - Order: {}",
                        transaction.getUserId(), orderId);
                result.setSuccess(false);
                result.setMessage("Payment failed");
            }

            paymentRepository.save(transaction);

            // Fulfill the payment if successful (do this BEFORE audit logging so it doesn't get rolled back)
            if (isSuccessful) {
                fulfillmentService.fulfillPayment(orderId);
            }

            // Log billing audit (non-critical - catch exceptions to prevent rollback)
            try {
                billingAuditService.logWebhook(
                    getGatewayName(),
                    transaction.getUserId(),
                    null,
                    transaction.getPaymentKey(),
                    orderId,
                    isSuccessful ? "success" : "failed",
                    "callback",
                    result.getMessage(),
                    rawPayload,
                    null,
                    isSuccessful ? BillingAuditType.WEBHOOK_SUCCESS : BillingAuditType.WEBHOOK_FAILED
                );
            } catch (Exception auditEx) {
                log.warn("Failed to log billing audit for callback (non-critical): {}", auditEx.getMessage());
            }
            
            return result;

        } catch (Exception e) {
            log.error("Error handling Paymob payment callback: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
            return result;
        }
    }
    
    private boolean verifySignatureFromParams(Map<String, String> params, String expectedHmac) {
        try {
             // Paymob HMAC Calculation from Map (GET params)
             StringBuilder sb = new StringBuilder();
             sb.append(params.getOrDefault("amount_cents", ""));
             sb.append(params.getOrDefault("created_at", ""));
             sb.append(params.getOrDefault("currency", ""));
             sb.append(params.getOrDefault("error_occured", ""));
             sb.append(params.getOrDefault("has_parent_transaction", ""));
             sb.append(params.getOrDefault("id", ""));
             sb.append(params.getOrDefault("integration_id", ""));
             sb.append(params.getOrDefault("is_3d_secure", ""));
             sb.append(params.getOrDefault("is_auth", ""));
             sb.append(params.getOrDefault("is_capture", ""));
             sb.append(params.getOrDefault("is_refunded", ""));
             sb.append(params.getOrDefault("is_standalone_payment", ""));
             sb.append(params.getOrDefault("is_voided", ""));
             sb.append(params.getOrDefault("order", "")); 
             sb.append(params.getOrDefault("owner", ""));
             sb.append(params.getOrDefault("pending", ""));
             sb.append(params.getOrDefault("source_data.pan", ""));
             sb.append(params.getOrDefault("source_data.sub_type", ""));
             sb.append(params.getOrDefault("source_data.type", ""));
             sb.append(params.getOrDefault("success", ""));
             
             return checkHmac(sb.toString(), expectedHmac);
        } catch(Exception e) {
            log.error("HMAC check failed", e);
            return false;
        }
    }

    private boolean verifySignatureFromJson(JsonNode n, String expectedHmac) {
        try {
            // Paymob HMAC Calculation from JSON (POST body)
            StringBuilder sb = new StringBuilder();
            sb.append(getValue(n, "amount_cents"));
            sb.append(getValue(n, "created_at"));
            sb.append(getValue(n, "currency"));
            sb.append(getValue(n, "error_occured"));
            sb.append(getValue(n, "has_parent_transaction"));
            sb.append(getValue(n, "id"));
            sb.append(getValue(n, "integration_id"));
            sb.append(getValue(n, "is_3d_secure"));
            sb.append(getValue(n, "is_auth"));
            sb.append(getValue(n, "is_capture"));
            sb.append(getValue(n, "is_refunded"));
            sb.append(getValue(n, "is_standalone_payment"));
            sb.append(getValue(n, "is_voided"));
            sb.append(getValue(n.path("order"), "id")); 
            sb.append(getValue(n, "owner"));
            sb.append(getValue(n, "pending"));
            sb.append(getValue(n.path("source_data"), "pan"));
            sb.append(getValue(n.path("source_data"), "sub_type"));
            sb.append(getValue(n.path("source_data"), "type"));
            sb.append(getValue(n, "success"));

            return checkHmac(sb.toString(), expectedHmac);

        } catch (Exception e) {
            log.warn("Paymob signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkHmac(String concatenated, String expected) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] digest = mac.doFinal(concatenated.getBytes(StandardCharsets.UTF_8));
        String calculated = bytesToHex(digest);
        return expected.equalsIgnoreCase(calculated);
    }

    private String getValue(JsonNode node, String key) {
        if (node == null || node.isNull() || !node.has(key)) return "";
        JsonNode val = node.get(key);
        if (val.isBoolean()) return val.asBoolean() ? "true" : "false";
        return val.asText();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String getPaymentUrl(String paymentKey) {
        if (iframeId != null && !iframeId.isEmpty()) {
            return String.format("https://accept.paymob.com/api/acceptance/iframes/%s?payment_token=%s",
                    iframeId, paymentKey);
        }
        // Default iframe URL
        return String.format("https://accept.paymob.com/api/acceptance/iframes/IFRAME_ID?payment_token=%s", paymentKey);
    }

    /**
     * Authenticate with Paymob API
     */
    private String authenticate() {
        try {
            String payload = String.format("{\"api_key\":\"%s\"}", apiKey);
            String response = paymobClient.authenticate(payload);

            PaymobAuthResponse authResponse = objectMapper.readValue(response, PaymobAuthResponse.class);
            return authResponse.getToken();

        } catch (Exception e) {
            log.error("Failed to authenticate with Paymob: {}", e.getMessage(), e);
            throw new RuntimeException("Paymob authentication failed", e);
        }
    }

    /**
     * Create order in Paymob
     */
    private String createOrder(String authToken, double amount) {
        try {
            // Amount should be in cents (smallest currency unit)
            int amountCents = (int) (amount * 100);

            String payload = String.format(
                    "{\"auth_token\":\"%s\",\"delivery_needed\":\"false\",\"amount_cents\":\"%d\",\"currency\":\"EGP\",\"items\":[]}",
                    authToken, amountCents
            );

            String response = paymobClient.createOrder("Bearer " + authToken, payload);

            PaymobOrderResponse orderResponse = objectMapper.readValue(response, PaymobOrderResponse.class);
            return String.valueOf(orderResponse.getId());

        } catch (Exception e) {
            log.error("Failed to create Paymob order: {}", e.getMessage(), e);
            throw new RuntimeException("Paymob order creation failed", e);
        }
    }

    /**
     * Create payment key for the order
     */
    private String createPaymentKey(String authToken, String orderId, double amount, String integrationId, Map<String, Object> metadata) {
        try {
            int amountCents = (int) (amount * 100);

            // Build billing data from metadata or use defaults
            String billingData = buildBillingData(metadata);

            String payload = String.format(
                    "{\"auth_token\":\"%s\",\"amount_cents\":\"%d\",\"expiration\":3600,\"order_id\":\"%s\"," +
                    "\"billing_data\":%s,\"currency\":\"EGP\",\"integration_id\":%s}",
                    authToken, amountCents, orderId, billingData, integrationId
            );

            String response = paymobClient.createPaymentKey("Bearer " + authToken, payload);

            PaymobPaymentKeyResponse keyResponse = objectMapper.readValue(response, PaymobPaymentKeyResponse.class);
            return keyResponse.getToken();

        } catch (Exception e) {
            log.error("Failed to create Paymob payment key: {}", e.getMessage(), e);
            throw new RuntimeException("Paymob payment key creation failed", e);
        }
    }

    /**
     * Process wallet payment to get redirect URL
     */
    private String processWalletPayment(String paymentKey, String phoneNumber) {
        try {
            String payload = String.format(
                "{\"source\":{\"identifier\":\"%s\",\"subtype\":\"WALLET\"},\"payment_token\":\"%s\"}",
                phoneNumber, paymentKey
            );

            log.debug("Processing wallet payment with payload: {}", payload);
            String response = paymobClient.processPayment(payload);
            log.debug("Wallet payment response: {}", response);

            JsonNode root = objectMapper.readTree(response);
            
            // Paymob returns 'redirect_url' or 'iframe_url' or just 'url' in some versions
            if (root.has("redirect_url") && !root.get("redirect_url").isNull()) {
                return root.get("redirect_url").asText();
            } else if (root.has("url") && !root.get("url").isNull()) {
                 return root.get("url").asText();
            } else if (root.has("iframe_url") && !root.get("iframe_url").isNull()) {
                 return root.get("iframe_url").asText();
            }
            
            throw new RuntimeException("No redirect URL found in Paymob response");

        } catch (Exception e) {
            log.error("Failed to process wallet payment: {}", e.getMessage(), e);
            throw new RuntimeException("Wallet payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build billing data JSON from metadata
     */
    private String buildBillingData(Map<String, Object> metadata) {
        String firstName = "Customer";
        String lastName = "User";
        String email = "customer@example.com";
        String phone = "+201000000000";

        if (metadata != null) {
            firstName = (String) metadata.getOrDefault("firstName", firstName);
            lastName = (String) metadata.getOrDefault("lastName", lastName);
            email = (String) metadata.getOrDefault("email", email);
            phone = (String) metadata.getOrDefault("phone", phone);
        }

        return String.format(
                "{\"apartment\":\"NA\",\"email\":\"%s\",\"floor\":\"NA\",\"first_name\":\"%s\"," +
                "\"street\":\"NA\",\"building\":\"NA\",\"phone_number\":\"%s\",\"shipping_method\":\"NA\"," +
                "\"postal_code\":\"NA\",\"city\":\"NA\",\"country\":\"NA\",\"last_name\":\"%s\",\"state\":\"NA\"}",
                email, firstName, phone, lastName
        );
    }

    /**
     * Parse success from callback data (JsonNode)
     */
    private boolean parseCallbackSuccess(JsonNode callbackData) {
        if (callbackData.has("success")) {
             return callbackData.get("success").asBoolean();
        }
        if (callbackData.has("is_success")) {
             return callbackData.get("is_success").asBoolean();
        }
        
        // Also check obj.success nested field
        if (callbackData.has("obj")) {
            JsonNode obj = callbackData.get("obj");
            if (obj.has("success")) {
                return obj.get("success").asBoolean();
            }
        }

        return false;
    }

    /**
     * Extract order ID from callback data node
     */
    private String extractOrderId(JsonNode orderIdNode) {
        if (orderIdNode == null || orderIdNode.isNull()) {
            return null;
        }

        if (orderIdNode.isTextual()) {
            return orderIdNode.asText();
        }
        if (orderIdNode.isNumber()) {
            return String.valueOf(orderIdNode.asLong());
        }
        
        // Nested object with id field
        if (orderIdNode.isObject() && orderIdNode.has("id")) {
             return orderIdNode.get("id").asText();
        }

        return null;
    }

    @Override
    public Long getPaymentMethod(PaymentMethods paymentMethod){
        if (paymentMethod == null) {
            log.warn("Payment method passed to Paymob is NULL. Defaulting to CARD integration ID.");
            return cardIntegrationId;
        }
        switch (paymentMethod){
            case CARD -> {
                return cardIntegrationId;
            }
            case WALLET -> {
                return walletIntegrationId;
            }
            case FAWRY -> {
                log.warn("FAWRY payment method is not yet implemented for Paymob gateway.");
                throw new UnsupportedOperationException("FAWRY payment method is not supported via Paymob");
            }
            default -> {
                log.warn("Unknown payment method: {}.", paymentMethod);
                throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
            }
        }
    }
}
