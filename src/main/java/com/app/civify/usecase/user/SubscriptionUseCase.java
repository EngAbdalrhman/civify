package com.app.civify.usecase.user;

import com.app.civify.dto.plan.SimplePlanDTO;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.dto.subscription.PromoValidationResponse;
import com.app.civify.dto.subscription.SubscriptionResponse;
import com.app.civify.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionUseCase {
    List<SimplePlanDTO> getAllActivePlans();

    SimplePlanDTO getPlanById(Long planId);

    PromoValidationResponse validatePromoCode(String code, BigDecimal amount);

    /**
     * Calculate subscription price with promo validation.
     * Used for checkout page - validates promo applicability and user usage.
     */
    PriceCalculationResponse calculatePrice(Long userId, Long planId, boolean isAnnual, String promoCode);

    SubscriptionResponse getActiveSubscription(Long userId);

    List<SubscriptionResponse> getUserSubscriptions(Long userId);

    /**
     * Cancel user's active non-free subscription.
     * Sets autoRenew = false so subscription stays active until endDate.
     *
     * @param userId The user ID
     * @throws ResourceNotFoundException if no active non-free subscription exists
     */
    void cancelActiveSubscription(Long userId);

    boolean canUseFeature(Long userId, String feature);
}
