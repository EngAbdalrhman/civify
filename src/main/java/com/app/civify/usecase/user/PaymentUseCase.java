package com.app.civify.usecase.user;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentVerificationResponse;

import java.util.List;

/**
 * UseCase interface for payment operations.
 * Follows clean architecture by separating controller logic from business logic.
 */
public interface PaymentUseCase {

    /**
     * Initiate a payment for a subscription plan
     *
     * @param userId    User ID initiating the payment
     * @param planId    Plan ID to subscribe to
     * @param isAnnual  Whether this is annual billing
     * @param promoCode Optional promo code
     * @param gateway   Optional payment gateway preference
     * @return Payment initiation response with payment URL
     */
    PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway);

    /**
     * Verify payment and activate subscription if successful.
     * Orchestrates verification and activation in a single operation.
     *
     * @param paymentId Payment transaction ID to verify
     * @return Verification result with status and message
     */
    PaymentVerificationResponse verifyAndActivatePayment(String paymentId);

    /**
     * Get list of enabled payment gateways
     *
     * @return List of enabled gateway names
     */
    List<String> getEnabledGateways();
}
