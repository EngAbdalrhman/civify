package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCreationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public PaymentCreationResult createPayment(Long userId, double amount, String gateway) {
        log.info("Creating payment for user {} with amount {} using gateway {}", userId, amount, gateway);
        return gatewayManager.createPayment(gateway, userId, amount, null);
    }

    @Override
    public boolean verifyPayment(String paymentId) {
        log.info("Verifying payment with ID {}", paymentId);
        return gatewayManager.verifyPayment(paymentId);
    }

    @Override
    public void handlePaymentCallback(String gateway, String orderId, String rawPayload, Map<String, String> headers) {
        log.info("Handling payment callback for gateway {} order {}", gateway, orderId);
        gatewayManager.handleCallback(gateway, rawPayload, headers);
    }

    @Override
    public List<String> getEnabledGateways() {
        return gatewayManager.getEnabledGateways();
    }
}

