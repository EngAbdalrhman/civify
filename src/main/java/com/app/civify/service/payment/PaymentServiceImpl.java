package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.plans.PaymentTransaction;
import com.app.civify.repository.plans.PaymentRepository;
import com.app.civify.service.plans.TokenPackageService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Lazy
    @Autowired
    private TokenPackageService tokenPackageService;

    @Lazy
    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, String gateway) {
        log.info("Creating payment for user {} with amount {} using gateway {}", userId, amount, gateway);
        return gatewayManager.createPayment(gateway, userId, amount, null);
    }

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata) {
        log.info("Creating payment for user {} with amount {} and metadata {}", userId, amount, metadata);
        // Use default gateway or determine from config where 'default' is needed
        // For now, assuming 'fawaterak' or 'paymob' is configured as default in manager or we pass null to let manager decide
        return gatewayManager.createPayment(null, userId, amount, metadata);
    }

    @Override
    public boolean verifyPayment(String paymentId) {
        log.info("Verifying payment with ID {}", paymentId);
        return gatewayManager.verifyPayment(paymentId);
    }

    @Override
    public void handlePaymentCallback(String gateway, String orderId, String rawPayload, Map<String, String> headers) {
        log.info("Handling payment callback for gateway {} order {}", gateway, orderId);
        PaymentCallbackResult result = gatewayManager.handleCallback(gateway, rawPayload, headers);
        
        if (result.isSuccess()) {
            try {
                // Fetch transaction to check metadata
                String effectiveOrderId = result.getOrderId() != null ? result.getOrderId() : orderId;
                PaymentTransaction transaction = paymentRepository.findByOrderId(effectiveOrderId)
                        .orElseThrow(() -> new com.app.civify.exception.ResourceNotFoundException("payment.transaction.notfound"));
                
                if (transaction.getMetadata() != null) {
                    Map<String, Object> meta = objectMapper.readValue(transaction.getMetadata(), Map.class);
                    
                    if ("TOKEN_TOPUP".equals(meta.get("type"))) {
                        // Using orderId as the payment reference to look up the TokenPurchase
                        log.info("Processing TOKEN_TOPUP for order: {}", orderId);
                        tokenPackageService.fulfillTokenPurchase(orderId);
                        return;
                    }
                }

                // Default: Try to activate subscription
                // Subscription is linked via paymentKey (transactionId)
                log.info("Processing SUBSCRIPTION activation for paymentKey: {}", transaction.getPaymentKey());
                subscriptionService.activateSubscription(transaction.getPaymentKey());
            } catch (Exception e) {
                log.error("Failed to process payment fulfillment: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public List<String> getEnabledGateways() {
        return gatewayManager.getEnabledGateways();
    }
}

