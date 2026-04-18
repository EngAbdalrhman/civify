package com.app.civify.service.payment;

import com.app.civify.entity.enums.TransactionType;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Manages multiple payment gateways using strategy pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayManager {
    private final PaymentGatewayFactory gatewayFactory;

    /**
     * Get a payment gateway by name
     */
    public PaymentGateway getGateway(String gatewayName) {
        return gatewayFactory.get(gatewayName);
    }

    /**
     * Get the default payment gateway
     */
    public PaymentGateway getDefaultGateway() {
        return gatewayFactory.getDefault();
    }

    /**
     * Get all registered gateways
     */
    public List<PaymentGateway> getAllGateways() {
        return gatewayFactory.getEnabledGateways()
                .stream()
                .map(gatewayFactory::get)
                .toList();
    }

    /**
     * Check if a gateway is available
     */
    public boolean isGatewayAvailable(String gatewayName) {
        return gatewayFactory.isAvailable(gatewayName);
    }

    /**
     * Create payment using default gateway
     */
    public PaymentCreationResult createPayment(
            Long userId,
            double amount,
            PaymentMethods method,
            TransactionType type,
            Map<String, Object> metadata
    ) {
        return gatewayFactory
                .getDefault()
                .createPayment(userId, amount, method, type, metadata);
    }

    /**
     * Create payment using specific gateway
     */
    public PaymentCreationResult createPayment(
            String gatewayName,
            Long userId,
            double amount,
            PaymentMethods method,
            TransactionType type,
            Map<String, Object> metadata
    ) {
        return gatewayFactory
                .get(gatewayName)
                .createPayment(userId, amount, method, type, metadata);
    }

    /**
     * Verify payment using default gateway
     */
    public boolean verifyPayment(String transactionId) {
        return gatewayFactory
                .getDefault()
                .verifyPayment(transactionId);
    }

    /**
     * Verify payment using specific gateway
     */
    public boolean verifyPayment(String gatewayName, String transactionId) {
        return gatewayFactory
                .get(gatewayName)
                .verifyPayment(transactionId);
    }

    /**
     * Handle callback using specific gateway
     */
    public PaymentCallbackResult handleCallback(
            String gatewayName,
            String rawPayload,
            Map<String, String> headers
    ) {
        return gatewayFactory
                .get(gatewayName)
                .handleCallback(rawPayload, headers);
    }

    /**
     * Handle redirect using specific gateway
     */
    public PaymentCallbackResult handleRedirect(
            String gatewayName,
            Map<String, String> params
    ) {
        return gatewayFactory
                .get(gatewayName)
                .handleRedirect(params);
    }

    /**
     * Check if redirect params indicate success
     */
    public boolean isRedirectSuccess(String gatewayName, Map<String, String> params) {
        return gatewayFactory
                .get(gatewayName)
                .isRedirectSuccess(params);
    }

    /**
     * Get list of enabled gateways
     */
    public List<String> getEnabledGateways() {
        return gatewayFactory.getEnabledGateways();
    }
}
