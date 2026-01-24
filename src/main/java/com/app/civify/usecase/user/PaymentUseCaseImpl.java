package com.app.civify.usecase.user;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.payment.PaymentVerificationResponse;
import com.app.civify.repository.billing.InvoiceRepository;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, PaymentMethods method) {
        log.debug("Initiating payment for user {} with plan {}", userId, planId);
        return subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode, gateway, method);
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
                log.error("Invoice not found for paymentId: {} and no gateway provided. Cannot verify payment.", paymentId);
                return PaymentVerificationResponse.builder()
                        .verified(false)
                        .message("Payment verification failed: Transaction not found and gateway not specified")
                        .build();
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
            // Try invoice-based payment first (renewals, billing)
            boolean isInvoicePayment = invoiceService.handleSuccessfulPayment(paymentId);
            
            if (!isInvoicePayment) {
                // Fall back to subscription-based payment (new plans)
                subscriptionService.activateSubscription(paymentId);
                log.info("Payment {} verified and subscription activated", paymentId);
            } else {
                log.info("Payment {} verified and invoice processed (renewal/billing)", paymentId);
            }
            
            return PaymentVerificationResponse.builder()
                    .verified(true)
                    .message("Payment verified successfully")
                    .build();
        }

        log.warn("Payment {} verification failed", paymentId);
        return PaymentVerificationResponse.builder()
                .verified(false)
                .message("Payment verification failed")
                .build();
    }

    @Override
    public List<String> getEnabledGateways() {
        return paymentService.getEnabledGateways();
    }
}
