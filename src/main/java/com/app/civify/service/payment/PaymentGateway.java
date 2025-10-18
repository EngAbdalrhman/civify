package com.app.civify.service.payment;

import java.util.Map;

/**
 * Strategy interface for payment gateway implementations
 */
public interface PaymentGateway {

    /**
     * Create a payment transaction
     *
     * @param userId User ID
     * @param amount Amount in the gateway's currency (e.g., cents for Paymob)
     * @param metadata Additional metadata for the payment
     * @return Payment key or transaction ID
     */
    String createPayment(Long userId, double amount, Map<String, Object> metadata);

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
     * @return true if callback processed successfully
     */
    boolean handleCallback(Map<String, Object> callbackData);

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
