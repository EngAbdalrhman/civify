package com.app.civify.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Payment service implementation using strategy pattern for multiple gateways
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentGatewayManager gatewayManager;

    @Override
    public String createPayment(Long userId, double amount) {
        log.info("Creating payment for user {} with amount {} using default gateway", userId, amount);
        return gatewayManager.createPayment(userId, amount, null);
    }

    @Override
    public boolean verifyPayment(String paymentId) {
        log.info("Verifying payment with ID {} using default gateway", paymentId);
        return gatewayManager.getDefaultGateway().verifyPayment(paymentId);
    }

    @Override
    public void handlePaymentCallback(String orderId, Map<String, Object> callbackData) {
        log.info("Handling payment callback for order {} using default gateway", orderId);
        gatewayManager.getDefaultGateway().handleCallback(callbackData);
    }
}
