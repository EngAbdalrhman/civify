package com.app.civify.usecase.user;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentVerificationResponse;
import com.app.civify.service.geo.GeoLocationResult;
import com.app.civify.service.geo.GeoLocationService;
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
    private final GeoLocationService geoLocationService;

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
    public PaymentInitiationResponse initiatePaymentWithIp(Long userId, Long planId, boolean isAnnual, 
            String promoCode, String gateway, String currency, String clientIp) {
        return initiatePaymentWithIp(userId, planId, isAnnual, promoCode, gateway, currency, clientIp, null);
    }

    @Override
    public PaymentInitiationResponse initiatePaymentWithIp(Long userId, Long planId, boolean isAnnual, 
            String promoCode, String gateway, String currency, String clientIp, String paymentMethod) {
        
        String detectedCurrency = detectCurrency(currency, clientIp, userId);
        
        log.debug("Initiating payment for user {} with plan {}, currency {}, paymentMethod {}", 
                userId, planId, detectedCurrency, paymentMethod);
        return subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode, gateway, detectedCurrency, paymentMethod);
    }

    /**
     * Detect currency based on explicit override or geolocation from client IP.
     * Business logic moved from PaymentController to follow clean architecture.
     */
    private String detectCurrency(String currency, String clientIp, Long userId) {
        if (currency != null && !currency.isEmpty()) {
            log.info("Using explicit currency override: {} for user {}", currency, userId);
            return currency;
        }

        try {
            GeoLocationResult geoLocation = geoLocationService.getLocation(clientIp);
            if (geoLocation != null && geoLocation.isSuccess() && geoLocation.getCurrency() != null) {
                log.info("Auto-detected currency {} from {} for user {}", 
                        geoLocation.getCurrency(), geoLocation.getCountryCode(), userId);
                return geoLocation.getCurrency();
            }
        } catch (Exception geoEx) {
            log.error("Geolocation service error for user {}: {}, using default currency EGP", 
                    userId, geoEx.getMessage());
        }

        log.warn("Geolocation failed for user {}, using default currency EGP", userId);
        return "EGP";
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

