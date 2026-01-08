package com.app.civify.usecase.user;

import com.app.civify.dto.plan.SimplePlanDTO;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.dto.subscription.PromoValidationResponse;
import com.app.civify.dto.subscription.SubscriptionResponse;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.mapper.SubscriptionPlanMapper;
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
    private final SubscriptionPlanMapper planMapper;

    @Override
    public List<SimplePlanDTO> getAllActivePlans() {
        return planMapper.toSimplePlans(subscriptionService.getAllActivePlans());
    }

    @Override
    public SimplePlanDTO getPlanById(Long planId) {
        SubscriptionPlan plan = subscriptionService.getPlanById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription Plan not found with id: " + planId));
        return planMapper.toSimplePlan(plan);
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
        Optional<UserSubscription> subOpt = subscriptionService.getActiveSubscription(userId);
        
        if (subOpt.isPresent()) {
            return mapToResponse(subOpt.get());
        }
        
        // No active subscription - return "Free Plan" fallback
        // Try to get Free plan from database, otherwise return minimal free tier response
        Optional<SubscriptionPlan> freePlanOpt = subscriptionService.getPlanByName("Free");
        
        return SubscriptionResponse.builder()
                .id(null)
                .userId(userId)
                .planId(freePlanOpt.map(SubscriptionPlan::getId).orElse(null))
                .planName("Free")
                .planDescription("Basic free tier with limited features")
                .cvLimit(freePlanOpt.map(SubscriptionPlan::getCvLimit).orElse(1))
                .currentCvCount(0)
                .startDate(null)
                .endDate(null)
                .status(com.app.civify.entity.enums.SubscriptionStatus.ACTIVE)
                .paymentTransactionId(null)
                .usageResetDate(null)
                .currentTokenUsage(0)
                .autoRenew(false)
                .build();
    }

    @Override
    public List<SubscriptionResponse> getUserSubscriptions(Long userId) {
        return subscriptionService.getUserSubscriptions(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelActiveSubscription(Long userId) {
        subscriptionService.cancelActiveSubscription(userId);
    }

    @Override
    public boolean canUseFeature(Long userId, String feature) {
        return subscriptionService.canUserUseFeature(userId, feature);
    }

    @Override
    public PriceCalculationResponse calculateTotalPrice(Long userId, Long planId, boolean isAnnual, String promoCode) {
        return subscriptionService.calculateTotalPrice(userId, planId, isAnnual, promoCode);
    }

    @Override
    public PriceCalculationResponse calculateTotalPrice(Long userId, Long planId, boolean isAnnual, String promoCode, String currency) {
        return subscriptionService.calculateTotalPrice(userId, planId, isAnnual, promoCode, currency);
    }


    @Override
    public com.app.civify.dto.payment.PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, String currency) {
        return subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode, gateway, currency);
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
                .currentCvCount(sub.getCurrentCvCount())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .paymentTransactionId(sub.getPaymentTransactionId())
                .usageResetDate(sub.getUsageResetDate())
                .currentTokenUsage(sub.getCurrentTokenUsage())
                .tokenLimit(sub.getPlan() != null ? sub.getPlan().getTokenLimit() : null)
                .autoRenew(sub.getAutoRenew())
                .build();
    }
}

