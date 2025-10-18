package com.app.civify.service.payment;

import com.app.civify.dto.payment.paymob.PaymobAuthResponse;
import com.app.civify.dto.payment.paymob.PaymobOrderResponse;
import com.app.civify.dto.payment.paymob.PaymobPaymentKeyResponse;
import com.app.civify.entity.PaymentTransaction;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.repository.PaymentRepository;
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
        return apiKey != null && !apiKey.isEmpty()
                && integrationId != null && !integrationId.isEmpty();
    }

    @Override
    @Transactional
    public String createPayment(Long userId, double amount, Map<String, Object> metadata) {
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

            return paymentKey;

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
    public boolean handleCallback(Map<String, Object> callbackData) {
        try {
            log.info("Handling Paymob payment callback");
            log.debug("Callback data: {}", callbackData);

            // Extract order ID from callback
            Object orderIdObj = callbackData.get("order");
            String orderId = extractOrderId(orderIdObj);

            if (orderId == null || orderId.isEmpty()) {
                log.warn("Order ID missing in callback data");
                return false;
            }

            PaymentTransaction transaction = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found for order: " + orderId));

            // Parse callback success status
            boolean isSuccessful = parseCallbackSuccess(callbackData);

            if (isSuccessful) {
                transaction.setStatus(PaymentStatus.COMPLETED);
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("Paymob payment callback processed successfully for user {} - Order: {}",
                        transaction.getUserId(), orderId);
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                log.warn("Paymob payment callback indicates failure for user {} - Order: {}",
                        transaction.getUserId(), orderId);
            }

            paymentRepository.save(transaction);
            return true;

        } catch (Exception e) {
            log.error("Error handling Paymob payment callback: {}", e.getMessage(), e);
            return false;
        }
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
     * Parse success from callback data
     */
    private boolean parseCallbackSuccess(Map<String, Object> callbackData) {
        // Paymob sends "success" as string or boolean
        Object successObj = callbackData.get("success");
        if (successObj != null) {
            if (successObj instanceof Boolean) {
                return (Boolean) successObj;
            }
            if (successObj instanceof String) {
                return "true".equalsIgnoreCase((String) successObj);
            }
        }

        // Also check obj.success nested field
        Object obj = callbackData.get("obj");
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> objMap = (Map<String, Object>) obj;
            Object objSuccess = objMap.get("success");
            if (objSuccess != null) {
                if (objSuccess instanceof Boolean) {
                    return (Boolean) objSuccess;
                }
                if (objSuccess instanceof String) {
                    return "true".equalsIgnoreCase((String) objSuccess);
                }
            }
        }

        return false;
    }

    /**
     * Extract order ID from callback data (can be nested or direct)
     */
    private String extractOrderId(Object orderIdObj) {
        if (orderIdObj == null) {
            return null;
        }

        // Direct string or number
        if (orderIdObj instanceof String) {
            return (String) orderIdObj;
        }
        if (orderIdObj instanceof Number) {
            return orderIdObj.toString();
        }

        // Nested object with id field
        if (orderIdObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderMap = (Map<String, Object>) orderIdObj;
            Object id = orderMap.get("id");
            if (id != null) {
                return id.toString();
            }
        }

        return null;
    }

    /**
     * Get Paymob iframe URL for payment
     */
    public String getPaymentUrl(String paymentKey) {
        if (iframeId != null && !iframeId.isEmpty()) {
            return String.format("https://accept.paymob.com/api/acceptance/iframes/%s?payment_token=%s",
                    iframeId, paymentKey);
        }
        // Default iframe URL
        return String.format("https://accept.paymob.com/api/acceptance/iframes/IFRAME_ID?payment_token=%s", paymentKey);
    }
}
