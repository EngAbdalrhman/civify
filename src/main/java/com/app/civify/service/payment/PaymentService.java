package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCreationResult;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    /**
     * Create a payment transaction
     *
     * @param userId User ID
     * @param amount Amount
     * @param gateway Gateway name (optional)
     * @return PaymentCreationResult containing key and URL
     */
    PaymentCreationResult createPayment(Long userId, double amount, String gateway);

    /**
     * Create a payment transaction with metadata
     *
     * @param userId User ID
     * @param amount Amount
     * @param metadata Metadata (e.g. type=TOKEN_TOPUP)
     * @return PaymentCreationResult
     */
    PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata);

    /**
     * Verify a payment transaction
     *
     * @param paymentId Payment ID to verify
     * @return true if payment is successful
     */
    boolean verifyPayment(String paymentId);

    /**
     * Handle payment callback/webhook
     *
     * @param gateway Gateway name
     * @param orderId Order ID (optional, depending on gateway)
     * @param rawPayload Raw JSON payload
     * @param headers HTTP headers
     */
    void handlePaymentCallback(String gateway, String orderId, String rawPayload, Map<String, String> headers);

    /**
     * Get list of enabled gateways
     *
     * @return List of enabled gateway names
     */
    List<String> getEnabledGateways();
}
