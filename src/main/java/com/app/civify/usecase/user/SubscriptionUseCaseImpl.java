package com.app.civify.usecase.user;

import com.app.civify.dto.subscription.PromoValidationResponse;
import com.app.civify.dto.subscription.SubscriptionResponse;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.service.payment.PromoCodeService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUseCaseImpl extends CommonService implements SubscriptionUseCase {

    private final SubscriptionService subscriptionService;
    private final PromoCodeService promoCodeService;

    @Override
    public List<SubscriptionPlan> getAllActivePlans() {
        return subscriptionService.getAllActivePlans();
    }

    @Override
    public SubscriptionPlan getPlanById(Long planId) {
        return subscriptionService.getPlanById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription Plan not found with id: " + planId));
    }

    @Override
    public PromoValidationResponse validatePromoCode(String code, BigDecimal amount) {
        if (code == null || code.isBlank()) {
             return PromoValidationResponse.builder()
                     .valid(false)
                     .message("Promo code is required")
                     .build();
        }

        Optional<PromoCode> promoOpt = promoCodeService.validatePromoCode(code, amount);

        if (promoOpt.isEmpty()) {
            return PromoValidationResponse.builder()
                    .valid(false)
                    .message("Invalid or expired promo code")
                    .build();
        }

        PromoCode promo = promoOpt.get();
        BigDecimal discountAmount = promo.calculateDiscount(amount);

        return PromoValidationResponse.builder()
                .valid(true)
                .discountType(promo.getDiscountType().name())
                .discountValue(promo.getDiscountValue())
                .discountAmount(discountAmount)
                .message("Promo code applied successfully")
                .build();
    }

    @Override
    public SubscriptionResponse getActiveSubscription(Long userId) {
        UserSubscription sub = subscriptionService.getActiveSubscription(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found"));
        return mapToResponse(sub);
    }

    @Override
    public List<SubscriptionResponse> getUserSubscriptions(Long userId) {
        return subscriptionService.getUserSubscriptions(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelSubscription(Long subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
    }

    @Override
    public void renewSubscription(Long subscriptionId, boolean isAnnual) {
        subscriptionService.renewSubscription(subscriptionId, isAnnual);
    }

    @Override
    public boolean canUseFeature(Long userId, String feature) {
        return subscriptionService.canUserUseFeature(userId, feature);
    }

    private SubscriptionResponse mapToResponse(UserSubscription sub) {
        if (sub == null) return null;
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .userId(sub.getUser() != null ? sub.getUser().getId() : null)
                .planId(sub.getPlan() != null ? sub.getPlan().getId() : null)
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : null)
                .planDescription(sub.getPlan() != null ? sub.getPlan().getDescription() : null)
                .cvLimit(sub.getPlan() != null ? sub.getPlan().getCvLimit() : null)
                .apiLimit(sub.getPlan() != null ? sub.getPlan().getApiLimit() : null)
                .currentCvCount(sub.getCurrentCvCount())
                .currentApiCount(sub.getCurrentApiCount())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .paymentTransactionId(sub.getPaymentTransactionId())
                .usageResetDate(sub.getUsageResetDate())
                .currentTokenUsage(sub.getCurrentTokenUsage())
                .build();
    }
}
