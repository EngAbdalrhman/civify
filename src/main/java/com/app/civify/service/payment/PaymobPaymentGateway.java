package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.paymob.PaymobAuthResponse;
import com.app.civify.dto.payment.paymob.PaymobOrderResponse;
import com.app.civify.dto.payment.paymob.PaymobPaymentKeyResponse;
import com.app.civify.entity.plans.PaymentTransaction;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.plans.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@RequiredArgsConstructor
@Slf4j
public class PaymobPaymentGateway implements PaymentGateway {

    private final PaymobClient paymobClient;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.paymob.api-key}")
    private String apiKey;

    @Value("${payment.paymob.integration-id}")
    private String integrationId;

    @Value("${payment.paymob.iframe-id:}")
    private String iframeId;

    @Value("${payment.paymob.hmac-secret:}")
    private String hmacSecret;

    @Override
    public String getGatewayName() {
        return "paymob";
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.contains("your_paymob_api_key_here")
                && integrationId != null && !integrationId.isEmpty() && !integrationId.contains("your_integration_id_here");
    }

    @Override
    @Transactional
    public PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata) {
        if (!isEnabled()) {
            throw new RuntimeException("Paymob gateway is not configured properly. Please check api-key and integration-id in application.properties");
        }

        try {
            log.info("Creating Paymob payment for user {} with amount {} EGP", userId, amount);

            // Step 1: Authenticate
            String authToken = authenticate();
            log.debug("Paymob authentication successful");

            // Step 2: Create order
            String orderId = createOrder(authToken, amount);
            log.debug("Paymob order created with ID: {}", orderId);

            // Step 3: Create payment key
            String paymentKey = createPaymentKey(authToken, orderId, amount, metadata);
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
            String paymentUrl = getPaymentUrl(paymentKey);

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
        try {
            log.info("Verifying Paymob payment with key: {}", paymentKey);

            PaymentTransaction transaction = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found for key: " + paymentKey));

            // Authenticate and check payment status from Paymob
            String authToken = authenticate();
            String intentionResponse = paymobClient.getIntention(authToken, transaction.getOrderId());

            // Parse response to check if payment is successful
            boolean isSuccessful = parsePaymentStatus(intentionResponse);

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

        } catch (Exception e) {
            log.error("Error verifying Paymob payment {}: {}", paymentKey, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers) {
        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setGatewayName(getGatewayName());

        try {
            log.info("Handling Paymob payment callback");
            
            if (!verifySignature(rawPayload, headers)) {
                log.warn("Paymob callback failed HMAC verification");
                result.setSuccess(false);
                result.setMessage("Invalid signature");
                return result;
            }

            JsonNode callbackData = objectMapper.readTree(rawPayload);
            log.debug("Callback data: {}", callbackData);

            // Extract order ID from callback
            String orderId = extractOrderId(callbackData.get("order"));

            if (orderId == null || orderId.isEmpty()) {
                log.warn("Order ID missing in callback data");
                result.setSuccess(false);
                result.setMessage("Order ID missing");
                return result;
            }
            
            result.setOrderId(orderId);

            PaymentTransaction transaction = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found for order: " + orderId));

            // Idempotency: if already completed, short-circuit
            if (transaction.getStatus() == PaymentStatus.COMPLETED) {
                log.info("Paymob callback idempotent hit for order {}, already COMPLETED", orderId);
                result.setSuccess(true);
                result.setMessage("Already processed");
                return result;
            }
            
            result.setTransactionId(transaction.getPaymentKey());

            // Parse callback success status
            boolean isSuccessful = parseCallbackSuccess(callbackData);

            if (isSuccessful) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("Paymob payment callback processed successfully for user {} - Order: {}",
                        transaction.getUserId(), orderId);
                result.setSuccess(true);
                result.setMessage("Payment completed");
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                log.warn("Paymob payment callback indicates failure for user {} - Order: {}",
                        transaction.getUserId(), orderId);
                result.setSuccess(false);
                result.setMessage("Payment failed");
            }

            paymentRepository.save(transaction);
            return result;

        } catch (Exception e) {
            log.error("Error handling Paymob payment callback: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Error: " + e.getMessage());
            return result;
        }
    }

    private boolean verifySignature(String payload, Map<String, String> headers) {
        try {
            String expected = headers.getOrDefault("hmac", headers.getOrDefault("HMAC", null));
            if (expected == null || expected.isBlank()) {
                // Try query params if header not found (sometimes sent as 'hmac' param)
                // But for now, just return false if missing.
                return false;
            }

            JsonNode n = objectMapper.readTree(payload);
            
            // Paymob HMAC Calculation: Concatenate values of specific keys in order
            // keys: amount_cents, created_at, currency, error_occured, has_parent_transaction, id, 
            // integration_id, is_3d_secure, is_auth, is_capture, is_refunded, is_standalone_payment, 
            // is_voided, order.id, owner, pending, source_data.pan, source_data.sub_type, 
            // source_data.type, success
            
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
            sb.append(getValue(n.path("order"), "id")); // Nested order.id
            sb.append(getValue(n, "owner"));
            sb.append(getValue(n, "pending"));
            sb.append(getValue(n.path("source_data"), "pan"));
            sb.append(getValue(n.path("source_data"), "sub_type"));
            sb.append(getValue(n.path("source_data"), "type"));
            sb.append(getValue(n, "success"));

            String concatenated = sb.toString();
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            mac.init(new javax.crypto.spec.SecretKeySpec(hmacSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(concatenated.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String calculated = bytesToHex(digest);

            return expected.equalsIgnoreCase(calculated);

        } catch (Exception e) {
            log.warn("Paymob signature verification failed: {}", e.getMessage());
            return false;
        }
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
    private String createPaymentKey(String authToken, String orderId, double amount, Map<String, Object> metadata) {
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
     * Parse payment status from Paymob intention response
     */
    private boolean parsePaymentStatus(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // Check various success indicators
            if (root.has("success") && root.get("success").asBoolean()) {
                return true;
            }

            if (root.has("is_success") && root.get("is_success").asBoolean()) {
                return true;
            }

            if (root.has("status")) {
                String status = root.get("status").asText().toLowerCase();
                return status.equals("completed") || status.equals("success");
            }

            return false;

        } catch (Exception e) {
            log.error("Failed to parse payment status: {}", e.getMessage());
            return false;
        }
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


}
