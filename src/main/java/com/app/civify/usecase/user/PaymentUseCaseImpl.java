package com.app.civify.usecase.user;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentVerificationResponse;
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

    @Override
    public PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway) {
        log.debug("Initiating payment for user {} with plan {}", userId, planId);
        return subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode, gateway);
    }

    @Override
    public PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, String currency) {
        log.debug("Initiating payment for user {} with plan {} and currency {}", userId, planId, currency);
        return subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode, gateway, currency);
    }

    @Override
    public PaymentVerificationResponse verifyAndActivatePayment(String paymentId) {
        log.debug("Verifying and activating payment: {}", paymentId);

        boolean isVerified = paymentService.verifyPayment(paymentId);

        if (isVerified) {
            subscriptionService.activateSubscription(paymentId);
            log.info("Payment {} verified and subscription activated", paymentId);
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
