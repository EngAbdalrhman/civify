package com.app.civify.service.payment;

import com.app.civify.dto.payment.PaymentCallbackResult;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.plans.PaymentTransaction;
import com.app.civify.repository.plans.PaymentRepository;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.service.plans.TokenPackageService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
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

    @Lazy
    @Autowired
    private InvoiceService invoiceService;

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, String gateway) {
        log.info("Creating payment for user {} with amount {} using gateway {}", userId, amount, gateway);
        return gatewayManager.createPayment(gateway, userId, amount, null, null);
    }

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, Map<String, Object> metadata) {
        log.info("Creating payment for user {} with amount {} and metadata {}", userId, amount, metadata);
        return gatewayManager.createPayment(null, userId, amount, null, metadata);
    }

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, String gateway, Map<String, Object> metadata) {
        log.info("Creating payment for user {} with amount {} using gateway {} and metadata {}", userId, amount, gateway, metadata);
        return gatewayManager.createPayment(gateway, userId, amount, null, metadata);
    }

    @Override
    public PaymentCreationResult createPayment(Long userId, double amount, String currency, String gateway, Map<String, Object> metadata) {
        log.info("Creating payment for user {} with amount {} {} using gateway {} and metadata {}", userId, amount, currency, gateway, metadata);
        return gatewayManager.createPayment(gateway, userId, amount, currency, metadata);
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

                // Handle invoice payment (for renewals or initial invoices)
                boolean invoiceHandled = invoiceService.handleSuccessfulPayment(transaction.getPaymentKey());
                
                if (!invoiceHandled) {
                    // No invoice found - try to activate subscription (initial purchase flow)
                    log.info("Processing SUBSCRIPTION activation for paymentKey: {}", transaction.getPaymentKey());
                    subscriptionService.activateSubscription(transaction.getPaymentKey());
                } else {
                    log.info("Invoice payment processed for paymentKey: {}", transaction.getPaymentKey());
                }
            } catch (Exception e) {
                log.error("Failed to process payment fulfillment: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public List<String> getEnabledGateways() {
        return gatewayManager.getEnabledGateways();
    }

    @Override
    public String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

