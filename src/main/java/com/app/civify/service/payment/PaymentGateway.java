package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import java.util.Map;
import java.util.Set;

/**
 * Payment gateway interface for unified payment processing
 */
public interface PaymentGateway {

    /**
     * Get the set of currencies supported by this payment gateway
     *
     * @return Set of ISO 4217 currency codes (e.g., "EGP", "USD", "EUR")
     */
    Set<String> getSupportedCurrencies();

    /**
     * Create a payment transaction
     *
     * @param userId User ID
     * @param amount Amount in the gateway's currency
     * @param currency Currency code (e.g. "USD", "EGP")
     * @param metadata Additional metadata for the payment
     * @return PaymentCreationResult containing key and URL
     */
    PaymentCreationResult createPayment(Long userId, double amount, String currency, Map<String, Object> metadata);

    /**
     * Create a payment transaction with specific payment method
     *
     * @param userId User ID
     * @param amount Amount in the gateway's currency
     * @param currency Currency code (e.g. "USD", "EGP")
     * @param paymentMethod Payment method (e.g. "CARD", "FAWRY", "MOBILE_WALLET")
     * @param metadata Additional metadata for the payment
     * @return PaymentCreationResult containing key and URL
     */
    default PaymentCreationResult createPayment(Long userId, double amount, String currency, String paymentMethod, Map<String, Object> metadata) {
        // Default implementation ignores paymentMethod - gateways can override
        return createPayment(userId, amount, currency, metadata);
    }

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
    String getGatewayName();

    /**
     * Check if the gateway is enabled
     *
     * @return true if enabled
     */
    boolean isEnabled();
}
