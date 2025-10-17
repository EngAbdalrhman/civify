package com.app.civify.service.payment;

import com.app.civify.entity.PaymentTransaction;
import com.app.civify.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymobClient paymobClient;
    private final PaymentRepository paymentRepository;

    @Value("${payment.paymob.api-key}")
    private String apiKey;

    @Value("${payment.paymob.integration-id}")
    private String integrationId;

    @Override
    @Transactional
    public String createPayment(Long userId, double amount) {
        try {
            log.info("Creating payment for user {} with amount {}", userId, amount);

            // Step 1: Authenticate with Paymob
            String authToken = authenticateWithPaymob();

            // Step 2: Create order
            String orderId = createOrder(authToken, amount);

            // Step 3: Create payment key
            String paymentKey = createPaymentKey(authToken, orderId, amount);

            // Step 4: Save transaction record
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setUserId(userId);
            transaction.setAmount(amount);
            transaction.setOrderId(orderId);
            transaction.setPaymentKey(paymentKey);
            transaction.setStatus("PENDING");
            paymentRepository.save(transaction);

            log.info("Payment created successfully for user {} with order ID {}", userId, orderId);
            return paymentKey;

        } catch (Exception e) {
            log.error("Error creating payment for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @Override
    @Transactional
    public boolean verifyPayment(String paymentId) {
        try {
            log.info("Verifying payment with ID {}", paymentId);

            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByPaymentKey(paymentId);
            if (transactionOpt.isEmpty()) {
                log.warn("Payment transaction not found for payment ID {}", paymentId);
                return false;
            }

            PaymentTransaction transaction = transactionOpt.get();

            // Authenticate and check payment status
            String authToken = authenticateWithPaymob();
            String intentionResponse = paymobClient.getIntention(authToken, transaction.getOrderId());

            // Parse response and update status
            boolean isSuccessful = parsePaymentStatus(intentionResponse);

            if (isSuccessful) {
                transaction.setStatus("COMPLETED");
                transaction.setCompletedAt(LocalDateTime.now());
                // Here you would upgrade user to LICENSED_USER role
                log.info("Payment verified successfully for user {}", transaction.getUserId());
            } else {
                transaction.setStatus("FAILED");
                log.warn("Payment verification failed for user {}", transaction.getUserId());
            }

            paymentRepository.save(transaction);
            return isSuccessful;

        } catch (Exception e) {
            log.error("Error verifying payment {}: {}", paymentId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String orderId, Map<String, Object> callbackData) {
        try {
            log.info("Handling payment callback for order {}", orderId);

            Optional<PaymentTransaction> transactionOpt = paymentRepository.findByOrderId(orderId);
            if (transactionOpt.isEmpty()) {
                log.warn("Transaction not found for order ID {}", orderId);
                return;
            }

            PaymentTransaction transaction = transactionOpt.get();

            // Parse callback data
            String status = (String) callbackData.get("success");
            if ("true".equals(status)) {
                transaction.setStatus("COMPLETED");
                transaction.setCompletedAt(LocalDateTime.now());
                // Upgrade user role to LICENSED_USER
                upgradeUserToLicensed(transaction.getUserId());
                log.info("Payment callback processed successfully for user {}", transaction.getUserId());
            } else {
                transaction.setStatus("FAILED");
                log.warn("Payment callback indicates failure for user {}", transaction.getUserId());
            }

            paymentRepository.save(transaction);

        } catch (Exception e) {
            log.error("Error handling payment callback for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to process payment callback", e);
        }
    }

    private String authenticateWithPaymob() {
        String payload = "{\"api_key\":\"" + apiKey + "\"}";
        String response = paymobClient.authenticate(payload);
        // Parse token from response
        return "Bearer " + parseTokenFromResponse(response);
    }

    private String createOrder(String authToken, double amount) {
        String payload = String.format(
                "{\"amount\":%.2f,\"currency\":\"EGP\",\"items\":[],\"delivery_needed\":false}",
                amount * 100 // Convert to cents
        );
        String response = paymobClient.createOrder(authToken, payload);
        return parseOrderIdFromResponse(response);
    }

    private String createPaymentKey(String authToken, String orderId, double amount) {
        String payload = String.format(
                "{\"order_id\":\"%s\",\"amount\":%.2f,\"currency\":\"EGP\",\"integration_id\":%s,\"billing_data\":{\"first_name\":\"Test\",\"last_name\":\"User\",\"email\":\"test@example.com\"}}",
                orderId, amount * 100, integrationId
        );
        String response = paymobClient.createPaymentKey(authToken, payload);
        return parsePaymentKeyFromResponse(response);
    }

    private boolean parsePaymentStatus(String response) {
        // Parse JSON response to check payment status
        return response.contains("\"status\":\"completed\"") || response.contains("\"success\":true");
    }

    private String parseTokenFromResponse(String response) {
        // Parse token from authentication response
        return "dummy_token"; // Implement proper JSON parsing
    }

    private String parseOrderIdFromResponse(String response) {
        // Parse order ID from order creation response
        return "dummy_order_id"; // Implement proper JSON parsing
    }

    private String parsePaymentKeyFromResponse(String response) {
        // Parse payment key from payment key creation response
        return "dummy_payment_key"; // Implement proper JSON parsing
    }

    private void upgradeUserToLicensed(Long userId) {
        // Implement user role upgrade logic
        log.info("Upgrading user {} to LICENSED_USER role", userId);
        // This would typically involve updating the user entity
    }
}
