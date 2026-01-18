package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    /**
     * Create a payment transaction with gateway and metadata
     *
     * @param userId User ID
     * @param amount Amount
     * @param gateway Gateway name
     * @param metadata Metadata
     * @param method payment Method
     * @return PaymentCreationResult
     */
    PaymentCreationResult createPayment(Long userId, double amount, String gateway, PaymentMethods method, Map<String, Object> metadata);

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
     * Handle payment redirect (GET request from browser)
     */
    void handlePaymentRedirect(String gateway, Map<String, String> params);

    /**
     * Check if payment redirect parameters indicate success
     */
    boolean isRedirectSuccess(String gateway, Map<String, String> params);

    /**
     * Get list of enabled gateways
     *
     * @return List of enabled gateway names
     */
    List<String> getEnabledGateways();
}
