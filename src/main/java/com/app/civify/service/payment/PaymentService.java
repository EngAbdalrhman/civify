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
     * @param callbackData Callback data
     */
    void handlePaymentCallback(String gateway, String orderId, Map<String, Object> callbackData);

    /**
     * Get list of enabled gateways
     *
     * @return List of enabled gateway names
     */
    List<String> getEnabledGateways();
}
