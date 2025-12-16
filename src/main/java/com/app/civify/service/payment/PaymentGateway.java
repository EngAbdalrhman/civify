package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import java.util.Map;

/**
 * Strategy interface for payment gateway implementations
 */
public interface PaymentGateway {

    /**
     * Create a payment transaction
     *
     * @param userId User ID
     * @param amount Amount in the gateway's currency
     * @param metadata Additional metadata for the payment
     * @return PaymentCreationResult containing key and URL
     */
    PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata);

    /**
     * Verify a payment transaction
     *
     * @param transactionId Transaction ID to verify
     * @return true if payment is successful
     */
    boolean verifyPayment(String transactionId);

    /**
     * Handle payment callback/webhook
     *
     * @param callbackData Callback data from the gateway
     * @return PaymentCallbackResult with status and details
     */
    PaymentCallbackResult handleCallback(Map<String, Object> callbackData, Map<String, String> headers);

    /**
     * Get the payment URL for a given payment key/token
     *
     * @param paymentKey Payment key or token
     * @return Full URL to redirect the user to
     */
    String getPaymentUrl(String paymentKey);

    /**
     * Get the gateway name
     *
     * @return Gateway name
     */
    String getGatewayName();

    /**
     * Check if the gateway is enabled
     *
     * @return true if enabled
     */
    boolean isEnabled();
}
