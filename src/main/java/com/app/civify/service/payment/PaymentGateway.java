package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;

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
    PaymentCreationResult createPayment(Long userId, double amount, PaymentMethods method, com.app.civify.entity.enums.TransactionType type, Map<String, Object> metadata);

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
     * @param rawPayload Raw JSON payload from the webhook
     * @param headers HTTP headers
     * @return PaymentCallbackResult with status and details
     */
    PaymentCallbackResult handleCallback(String rawPayload, Map<String, String> headers);

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
    /**
     * Handle redirect from payment gateway (e.g. browser callback).
     * By default delegates to handleCallback if no specific logic needed,
     * but Paymob needs logic similar to callback but with query params.
     */
    default PaymentCallbackResult handleRedirect(Map<String, String> params) {
         throw new UnsupportedOperationException("Redirect handling not implemented for this gateway");
    }

    /**
     * Check if the redirect parameters indicate a successful payment.
     * This is used by the controller to determine where to redirect the user,
     * independently of the backend processing result.
     */
    default boolean isRedirectSuccess(Map<String, String> params) {
        return false;
    }

    String getGatewayName();

    /**
     * Check if the gateway is enabled
     *
     * @return true if enabled
     */
    boolean isEnabled();

    Long getPaymentMethod(PaymentMethods paymentMethods);

    /**
     * Charge a saved card token (MOTO / recurring).
     * @return true if charge succeeded
     */
    default boolean chargeWithToken(Long userId, double amount, String cardToken, Long subscriptionId) {
        throw new UnsupportedOperationException("Token charging not supported");
    }
}
