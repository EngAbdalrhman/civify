package com.app.civify.service.payment;

import com.app.civify.entity.plans.PaymentTransaction;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.plans.PaymentRepository;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.service.plans.TokenPackageService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service responsible for fulfilling successful payments.
 * This service is called by payment gateways after verifying a payment is successful.
 * It handles token top-ups, invoice payments, and subscription activations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFulfillmentService {

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

    /**
     * Fulfill a successful payment by order ID.
     * Determines the payment type and triggers the appropriate fulfillment action.
     *
     * @param orderId The order ID of the successful payment
     * @return true if fulfillment was successful
     */
    @Transactional
    public boolean fulfillPayment(String orderId) {
        log.info("Fulfilling payment for order: {}", orderId);

        try {
            PaymentTransaction transaction = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("payment.transaction.notfound"));

            return fulfillTransaction(transaction);
        } catch (Exception e) {
            log.error("Failed to fulfill payment for order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Fulfill a successful payment by payment key.
     *
     * @param paymentKey The payment key of the successful payment
     * @return true if fulfillment was successful
     */
    @Transactional
    public boolean fulfillPaymentByKey(String paymentKey) {
        log.info("Fulfilling payment for paymentKey: {}", paymentKey);

        try {
            PaymentTransaction transaction = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new ResourceNotFoundException("payment.transaction.notfound"));

            return fulfillTransaction(transaction);
        } catch (Exception e) {
            log.error("Failed to fulfill payment for key {}: {}", paymentKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Core fulfillment logic for a payment transaction.
     */
    private boolean fulfillTransaction(PaymentTransaction transaction) {
        try {
            // Check if this is a token top-up
            if (transaction.getMetadata() != null) {
                Map<String, Object> meta = objectMapper.readValue(transaction.getMetadata(), Map.class);

                if ("TOKEN_TOPUP".equals(meta.get("type"))) {
                    log.info("Processing TOKEN_TOPUP for order: {}", transaction.getOrderId());
                    tokenPackageService.fulfillTokenPurchase(transaction.getOrderId());
                    return true;
                }
            }

            // Try to handle invoice payment first (for renewals)
            boolean invoiceHandled = invoiceService.handleSuccessfulPayment(transaction.getPaymentKey());

            if (!invoiceHandled) {
                // No invoice found - activate subscription (initial purchase flow)
                log.info("Processing SUBSCRIPTION activation for paymentKey: {}", transaction.getPaymentKey());
                subscriptionService.activateSubscription(transaction.getPaymentKey());
            } else {
                log.info("Invoice payment processed for paymentKey: {}", transaction.getPaymentKey());
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to fulfill transaction {}: {}", transaction.getOrderId(), e.getMessage(), e);
            return false;
        }
    }
}
