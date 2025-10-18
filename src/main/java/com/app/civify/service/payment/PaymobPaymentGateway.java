package com.app.civify.service.payment;

import com.app.civify.entity.PaymentTransaction;
import com.app.civify.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Paymob payment gateway implementation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymobPaymentGateway implements PaymentGateway {

    private final PaymobClient paymobClient;
    private final PaymentRepository paymentRepository;

    @Value("${PAYMOB_API_KEY:}")
    private String apiKey;

    @Value("${PAYMOB_INTEGRATION_ID:}")
    private String integrationId;

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
            throw new RuntimeException("Paymob gateway is not configured");
        }

        try {
            log.info("Creating Paymob payment for user {} with amount {}", userId, amount);

            // Step 1: Authenticate with Paymob
            String authToken = authenticateWithPaymob();

            // Step 2: Create order
            String orderId = createOrder(authToken, amount);

            // Step 3: Create payment key
            String paymentKey = createPaymentKey(authToken, orderId, amount, metadata);

            // Step 4: Save transaction record
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setUserId(userId);
            transaction.setAmount(amount);
            transaction.setOrderId(orderId);
            transaction.setPaymentKey(paymentKey);
            transaction.setStatus("PENDING");
            transaction.setGateway(getGatewayName());

            // Store metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                transaction.setMetadata(metadata.toString());
            }

            paymentRepository.save(transaction);

            log.info("Paymob payment created successfully for user {} with order ID {}", userId, orderId);
            return paymentKey;

        } catch (Exception e) {
            log.error("Error creating Paymob payment for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create Paymob payment", e);
        }
    }

    @Override
    @Transactional
    public boolean verifyPayment(String transactionId) {
        try {
            log.info("Verifying Paymob payment with ID {}", transactionId);

            PaymentTransaction transaction = paymentRepository.findByPaymentKey(transactionId)
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

            // Authenticate and check payment status
            String authToken = authenticateWithPaymob();
            String intentionResponse = paymobClient.getIntention(authToken, transaction.getOrderId());

            // Parse response and update status
            boolean isSuccessful = parsePaymentStatus(intentionResponse);

            if (isSuccessful) {
                transaction.setStatus("COMPLETED");
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("Paymob payment verified successfully for user {}", transaction.getUserId());
            } else {
                transaction.setStatus("FAILED");
                log.warn("Paymob payment verification failed for user {}", transaction.getUserId());
            }

            paymentRepository.save(transaction);
            return isSuccessful;

        } catch (Exception e) {
            log.error("Error verifying Paymob payment {}: {}", transactionId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean handleCallback(Map<String, Object> callbackData) {
        try {
            log.info("Handling Paymob payment callback: {}", callbackData);

            String orderId = (String) callbackData.get("order_id");
            if (orderId == null) {
                log.warn("Order ID missing in callback data");
                return false;
            }

            PaymentTransaction transaction = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment transaction not found for order: " + orderId));

            // Parse callback data
            String success = (String) callbackData.get("success");
            boolean isSuccessful = "true".equals(success);

            if (isSuccessful) {
                transaction.setStatus("COMPLETED");
                transaction.setCompletedAt(LocalDateTime.now());
                log.info("Paymob payment callback processed successfully for user {}", transaction.getUserId());
            } else {
                transaction.setStatus("FAILED");
                log.warn("Paymob payment callback indicates failure for user {}", transaction.getUserId());
            }

            paymentRepository.save(transaction);
            return true;

        } catch (Exception e) {
            log.error("Error handling Paymob payment callback: {}", e.getMessage());
            return false;
        }
    }

    private String authenticateWithPaymob() {
        String payload = "{\"api_key\":\"" + apiKey + "\"}";
        String response = paymobClient.authenticate(payload);
        return "Bearer " + parseTokenFromResponse(response);
    }

    private String createOrder(String authToken, double amount) {
        String payload = String.format(
                "{\"amount\":%.0f,\"currency\":\"EGP\",\"items\":[],\"delivery_needed\":false}",
                amount * 100 // Convert to cents
        );
        String response = paymobClient.createOrder(authToken, payload);
        return parseOrderIdFromResponse(response);
    }

    private String createPaymentKey(String authToken, String orderId, double amount, Map<String, Object> metadata) {
        String billingData = buildBillingData(metadata);
        String payload = String.format(
                "{\"order_id\":\"%s\",\"amount\":%.0f,\"currency\":\"EGP\",\"integration_id\":%s,\"billing_data\":%s}",
                orderId, amount * 100, integrationId, billingData
        );
        String response = paymobClient.createPaymentKey(authToken, payload);
        return parsePaymentKeyFromResponse(response);
    }

    private String buildBillingData(Map<String, Object> metadata) {
        // Default billing data, can be enhanced with user data from metadata
        return "{\"first_name\":\"Customer\",\"last_name\":\"User\",\"email\":\"customer@example.com\"}";
    }

    private boolean parsePaymentStatus(String response) {
        return response.contains("\"status\":\"completed\"")
                || response.contains("\"success\":true")
                || response.contains("\"is_success\":true");
    }

    private String parseTokenFromResponse(String response) {
        // Parse JSON response to extract token
        // This should be implemented with proper JSON parsing
        int tokenIndex = response.indexOf("\"token\":\"");
        if (tokenIndex != -1) {
            int start = tokenIndex + 9;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        }
        throw new RuntimeException("Failed to parse auth token from response");
    }

    private String parseOrderIdFromResponse(String response) {
        // Parse JSON response to extract order ID
        int idIndex = response.indexOf("\"id\":");
        if (idIndex != -1) {
            int start = idIndex + 5;
            int end = response.indexOf(",", start);
            if (end == -1) {
                end = response.indexOf("}", start);
            }
            return response.substring(start, end).trim();
        }
        throw new RuntimeException("Failed to parse order ID from response");
    }

    private String parsePaymentKeyFromResponse(String response) {
        // Parse JSON response to extract payment key
        int keyIndex = response.indexOf("\"token\":\"");
        if (keyIndex != -1) {
            int start = keyIndex + 9;
            int end = response.indexOf("\"", start);
            return response.substring(start, end);
        }
        throw new RuntimeException("Failed to parse payment key from response");
    }
}
