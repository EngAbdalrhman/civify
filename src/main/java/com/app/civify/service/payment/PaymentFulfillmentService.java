package com.app.civify.service.payment;

import com.app.civify.entity.billing.BillingTransaction;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.service.plans.TokenPackageService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    private final BillingTransactionRepository paymentRepository;
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
     * IMPORTANT: Uses REQUIRES_NEW propagation to ensure this runs in a separate transaction.
     * This prevents fulfillment failures from rolling back the payment record.
     *
     * @param orderId The order ID of the successful payment
     * @return true if fulfillment was successful
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fulfillPayment(String orderId) {
        log.info("Fulfilling payment for order: {}", orderId);

        try {
            BillingTransaction transaction = paymentRepository.findByOrderId(orderId)
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
     * IMPORTANT: Uses REQUIRES_NEW propagation to ensure this runs in a separate transaction.
     *
     * @param paymentKey The payment key of the successful payment
     * @return true if fulfillment was successful
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fulfillPaymentByKey(String paymentKey) {
        log.info("Fulfilling payment for paymentKey: {}", paymentKey);

        try {
            BillingTransaction transaction = paymentRepository.findByPaymentKey(paymentKey)
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
    private boolean fulfillTransaction(BillingTransaction transaction) {
        try {
            switch (transaction.getType()) {
                case TOKEN_TOPUP:
                    log.info("Processing TOKEN_TOPUP for order: {}", transaction.getOrderId());
                    tokenPackageService.fulfillTokenPurchase(transaction.getOrderId());
                    return true;
                    
                case RENEWAL:
                    log.info("Processing RENEWAL for order: {}", transaction.getOrderId());
                    boolean invoiceHandled = invoiceService.handleSuccessfulPayment(transaction.getOrderId());
                    if (!invoiceHandled) {
                        invoiceHandled = invoiceService.handleSuccessfulPayment(transaction.getPaymentKey());
                    }
                    return invoiceHandled;
                    
                case SUBSCRIPTION:
                    log.info("Processing SUBSCRIPTION activation for orderId: {}", transaction.getOrderId());
                    subscriptionService.activateSubscription(transaction.getOrderId());
                    return true;
                    
                case REFUND:
                    log.info("Refund processing not yet implemented for order: {}", transaction.getOrderId());
                    return false;
                    
                default:
                    log.warn("Unknown transaction type {} for order {}", transaction.getType(), transaction.getOrderId());
                    return false;
            }
        } catch (Exception e) {
            log.error("Failed to fulfill transaction {}: {}", transaction.getOrderId(), e.getMessage(), e);
            return false;
        }
    }
}
