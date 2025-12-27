package com.app.civify.usecase.user;

import com.app.civify.dto.plan.SimplePlanDTO;
import com.app.civify.dto.subscription.PromoValidationResponse;
import com.app.civify.dto.subscription.SubscriptionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionUseCase {
    List<SimplePlanDTO> getAllActivePlans();
    SimplePlanDTO getPlanById(Long planId);
    PromoValidationResponse validatePromoCode(String code, BigDecimal amount);
    SubscriptionResponse getActiveSubscription(Long userId);
    List<SubscriptionResponse> getUserSubscriptions(Long userId);
    void cancelSubscription(Long subscriptionId);
    void renewSubscription(Long subscriptionId, boolean isAnnual);
    boolean canUseFeature(Long userId, String feature);
}
