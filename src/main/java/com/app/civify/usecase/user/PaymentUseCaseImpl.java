package com.app.civify.usecase.user;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.payment.PaymentVerificationResponse;
import com.app.civify.repository.billing.InvoiceRepository;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.service.payment.PaymentFulfillmentService;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentUseCaseImpl implements PaymentUseCase {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final BillingTransactionRepository paymentRepository;

    @Lazy
    @Autowired
    private PaymentFulfillmentService fulfillmentService;

    @Override
    public PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, PaymentMethods method, String phoneNumber) {
        return subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode, gateway, method, phoneNumber);
    }

    @Override
    public PaymentVerificationResponse verifyAndActivatePayment(String paymentId, String gateway) {
        log.debug("Verifying and activating payment: {}", paymentId);

        if (gateway == null || gateway.isEmpty()) {
            var invoiceOpt = invoiceRepository.findByPaymentTransactionId(paymentId);
            if (invoiceOpt.isPresent()) {
                gateway = invoiceOpt.get().getGateway();
                log.info("Found invoice {} for paymentId {}, using gateway: {}", invoiceOpt.get().getUuid(), paymentId, gateway);
            } else {
                // Fallback: check BillingTransaction table (covers token purchases and other non-invoice payments)
                var txnOpt = paymentRepository.findByOrderId(paymentId);
                if (txnOpt.isEmpty()) {
                    txnOpt = paymentRepository.findByPaymentKey(paymentId);
                }
                if (txnOpt.isPresent()) {
                    gateway = txnOpt.get().getGateway();
                    log.info("Found payment transaction for paymentId {}, using gateway: {}", paymentId, gateway);
                } else {
                    log.error("No invoice or payment transaction found for paymentId: {} and no gateway provided.", paymentId);
                    return PaymentVerificationResponse.builder()
                            .verified(false)
                            .message("Payment verification failed: Transaction not found and gateway not specified")
                            .build();
                }
            }
        } else {
            log.info("Using provided gateway for verification: {}", gateway);
        }

        if (gateway == null || gateway.isEmpty()) {
             log.error("Gateway could not be determined for paymentId: {}. Verification aborted.", paymentId);
             return PaymentVerificationResponse.builder()
                    .verified(false)
                    .message("Payment verification failed: Gateway could not be determined")
                    .build();
        }

        boolean isVerified = paymentService.verifyPayment(gateway, paymentId);

        if (isVerified) {
            String fulfillOrderId = paymentService.resolveFulfillmentOrderId(gateway, paymentId);
            boolean fulfilled = fulfillmentService.fulfillPayment(fulfillOrderId);
            if (!fulfilled) {
                fulfilled = fulfillmentService.fulfillPaymentByKey(paymentId);
            }

            if (fulfilled) {
                log.info("Payment {} verified and fulfilled successfully", paymentId);
                return PaymentVerificationResponse.builder()
                        .verified(true)
                        .message("Payment verified successfully")
                        .build();
            } else {
                log.warn("Payment {} verified but fulfillment failed. " +
                         "Webhook may handle fulfillment separately.", paymentId);
                return PaymentVerificationResponse.builder()
                        .verified(true)
                        .retryable(true)
                        .message("Payment verified but activation is still processing. Please wait a moment.")
                        .build();
            }
        }

        // Race condition guard: if a transaction row exists for this payment, the user actually
        // initiated checkout — the gateway may simply not have flipped to terminal state yet.
        // Tell the client to retry so the webhook has time to land.
        boolean transactionExists = paymentRepository.findByOrderId(paymentId).isPresent()
                || paymentRepository.findByPaymentKey(paymentId).isPresent();
        log.warn("Payment {} verification failed (gateway={}, transactionExists={})",
                paymentId, gateway, transactionExists);
        return PaymentVerificationResponse.builder()
                .verified(false)
                .retryable(transactionExists)
                .message(transactionExists
                        ? "Payment is still being processed. Please retry in a moment."
                        : "Payment verification failed")
                .build();
    }

    @Override
    public List<String> getEnabledGateways() {
        return paymentService.getEnabledGateways();
    }
}
