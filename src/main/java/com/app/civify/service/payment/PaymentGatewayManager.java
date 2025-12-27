package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple payment gateways using strategy pattern
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayManager {
    // TODO use factory + strategy + null object patterns

    private final Map<String, PaymentGateway> gateways = new ConcurrentHashMap<>();
    private final List<PaymentGateway> allGateways;
// TODO use @ConfigurationProperties
    @Value("${payment.default.gateway:fawaterak}")
    private String defaultGateway;

    @jakarta.annotation.PostConstruct
    public void init() {
        for (PaymentGateway gateway : allGateways) {
            registerGateway(gateway);
        }
    }

    /**
     * Register a payment gateway
     */
    public void registerGateway(PaymentGateway gateway) {
        if (gateway.isEnabled()) {
            gateways.put(gateway.getGatewayName(), gateway);
            log.info("Registered payment gateway: {}", gateway.getGatewayName());
        } else {
            log.warn("Payment gateway {} is not enabled, skipping registration", gateway.getGatewayName());
        }
    }

    /**
     * Get a payment gateway by name
     */
    public PaymentGateway getGateway(String gatewayName) {
        PaymentGateway gateway = gateways.get(gatewayName);
        if (gateway == null) {
            throw new IllegalArgumentException("Payment gateway not found: " + gatewayName);
        }
        return gateway;
    }

    /**
     * Get the default payment gateway
     */
    public PaymentGateway getDefaultGateway() {
        return getGateway(defaultGateway);
    }

    /**
     * Get all registered gateways
     */
    public List<PaymentGateway> getAllGateways() {
        return List.copyOf(gateways.values());
    }

    /**
     * Check if a gateway is available
     */
    public boolean isGatewayAvailable(String gatewayName) {
        PaymentGateway gateway = gateways.get(gatewayName);
        return gateway != null && gateway.isEnabled();
    }

    /**
     * Create payment using default gateway
     */
    public PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata) {
        return getDefaultGateway().createPayment(userId, amount, metadata);
    }

    /**
     * Create payment using specific gateway
     */
    public PaymentCreationResult createPayment(String gatewayName, Long userId, double amount, Map<String, Object> metadata) {
        // use null object pattern
        if (gatewayName == null || gatewayName.isEmpty()) {
            return createPayment(userId, amount, metadata);
        }
        return getGateway(gatewayName).createPayment(userId, amount, metadata);
    }

    /**
     * Verify payment using default gateway
     */
    public boolean verifyPayment(String transactionId) {
        // TODO  take care you will depend only here on fwaterek
        // Try to determine gateway from transaction ID or use default
        return getDefaultGateway().verifyPayment(transactionId);
    }

    /**
     * Handle callback using specific gateway
     */
    public PaymentCallbackResult handleCallback(String gatewayName, String rawPayload, Map<String, String> headers) {
        if (gatewayName == null || gatewayName.isEmpty()) {
            // Try to determine gateway from callback data or use default
            // This is tricky without gateway name, so we might default to Paymob or try all?
            // For now, let's use default
            return getDefaultGateway().handleCallback(rawPayload, headers);
        }
        return getGateway(gatewayName).handleCallback(rawPayload, headers);
    }

    /**
     * Get list of enabled gateways
     */
    public List<String> getEnabledGateways() {
        return gateways.values().stream()
                .filter(PaymentGateway::isEnabled)
                .map(PaymentGateway::getGatewayName)
                .toList();
    }
}
