package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Payment service implementation using strategy pattern for multiple gateways.
 * 
 * Fulfillment logic (token top-up, subscription activation, invoice handling) 
 * is delegated to individual payment gateways via PaymentFulfillmentService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentGatewayManager gatewayManager;

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, String gateway, PaymentMethods method, Map<String, Object> metadata) {
        log.info("Creating payment for user {} with amount {} using gateway {} and metadata {}", userId, amount, gateway, metadata);
        return gatewayManager.createPayment(gateway, userId, amount, method, metadata);
    }

    @Override
    public boolean verifyPayment(String paymentId) {
        log.info("Verifying payment with ID {} using default gateway", paymentId);
        return gatewayManager.verifyPayment(paymentId);
    }

    @Override
    public boolean verifyPayment(String gateway, String paymentId) {
        log.info("Verifying payment with ID {} using gateway {}", paymentId, gateway);
        return gatewayManager.verifyPayment(gateway, paymentId);
    }

    @Override
    public void handlePaymentCallback(String gateway, String orderId, String rawPayload, Map<String, String> headers) {
        log.info("Handling payment callback for gateway {} order {}", gateway, orderId);
        
        // Gateway handles callback processing and fulfillment internally
        PaymentCallbackResult result = gatewayManager.handleCallback(gateway, rawPayload, headers);
        
        if (result.isSuccess()) {
            log.info("Payment callback processed successfully for gateway {} order {}", gateway, result.getOrderId());
        } else {
            log.warn("Payment callback failed for gateway {}: {}", gateway, result.getMessage());
        }
    }

    @Override
    public void handlePaymentRedirect(String gateway, Map<String, String> params) {
        log.info("Handling payment redirect for gateway {}", gateway);
        
        // Gateway handles redirect processing and fulfillment internally
        PaymentCallbackResult result = gatewayManager.handleRedirect(gateway, params);
        
        if (result.isSuccess()) {
            log.info("Payment redirect processed successfully for gateway {} order {}", gateway, result.getOrderId());
        } else {
            log.warn("Payment redirect failed for gateway {}: {}", gateway, result.getMessage());
        }
    }

    @Override
    public boolean isRedirectSuccess(String gateway, Map<String, String> params) {
        return gatewayManager.isRedirectSuccess(gateway, params);
    }

    @Override
    public List<String> getEnabledGateways() {
        return gatewayManager.getEnabledGateways();
    }
}
