package com.app.civify.service.subscription;

import com.app.civify.entity.*;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.*;
import com.app.civify.security.model.Role;
import com.app.civify.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Override
    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findByIsDeletedFalse();
    }

    @Override
    public Optional<SubscriptionPlan> getPlanById(Long planId) {
        return planRepository.findById(planId);
    }

    @Override
    public Optional<SubscriptionPlan> getPlanByName(String name) {
        return planRepository.findByName(name);
    }

    @Override
    @Transactional
    public UserSubscription createSubscription(Long userId, Long planId, String promoCode, boolean isAnnual) {
        log.info("Creating subscription for user {} with plan {}", userId, planId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        // Check if user already has an active subscription
        Optional<UserSubscription> existingSubscription = getActiveSubscription(userId);
        if (existingSubscription.isPresent()) {
            throw new BadRequestException("User already has an active subscription");
        }

        // Create new subscription with PENDING status
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(calculateEndDate(isAnnual));
        subscription.setStatus(UserSubscription.SubscriptionStatus.PENDING);
        subscription.setCurrentCvCount(0);
        subscription.setCurrentApiCount(0);

        // Apply promo code if provided
        if (promoCode != null && !promoCode.isEmpty()) {
            PromoCode promo = promoCodeRepository.findByCodeAndActiveTrue(promoCode)
                    .orElseThrow(() -> new BadRequestException("Invalid or expired promo code"));

            if (!promo.isValid()) {
                throw new BadRequestException("Promo code is not valid");
            }

            subscription.setPromoCode(promo);
        }

        return subscriptionRepository.save(subscription);
    }

    @Override
    public Optional<UserSubscription> getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, UserSubscription.SubscriptionStatus.ACTIVE)
                .stream()
                .filter(UserSubscription::isActive)
                .findFirst();
    }

    @Override
    public List<UserSubscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    @Override
    public BigDecimal calculateTotalPrice(Long planId, boolean isAnnual, String promoCode) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        BigDecimal basePrice = isAnnual ? plan.getAnnualPrice() : plan.getMonthlyPrice();

        // Apply promo code discount if provided
        if (promoCode != null && !promoCode.isEmpty()) {
            Optional<PromoCode> promo = promoCodeRepository.findByCodeAndActiveTrue(promoCode);
            if (promo.isPresent() && promo.get().isValid()) {
                BigDecimal discount = promo.get().calculateDiscount(basePrice);
                basePrice = basePrice.subtract(discount);
            }
        }

        return basePrice;
    }

    @Override
    @Transactional
    public String initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode) {
        log.info("Initiating payment for user {} with plan {}", userId, planId);

        // Create subscription first
        UserSubscription subscription = createSubscription(userId, planId, promoCode, isAnnual);

        // Calculate total price
        BigDecimal totalPrice = calculateTotalPrice(planId, isAnnual, promoCode);

        // Create payment transaction
        String paymentKey = paymentService.createPayment(userId, totalPrice.doubleValue());

        // Link payment to subscription
        subscription.setPaymentTransactionId(paymentKey);
        subscriptionRepository.save(subscription);

        log.info("Payment initiated successfully for user {} with payment key {}", userId, paymentKey);
        return paymentKey;
    }

    @Override
    public boolean canUserUseFeature(Long userId, String feature) {
        Optional<UserSubscription> subscription = getActiveSubscription(userId);

        if (subscription.isEmpty()) {
            return false;
        }

        UserSubscription sub = subscription.get();
        SubscriptionPlan plan = sub.getPlan();

        return switch (feature.toUpperCase()) {
            case "CV" ->
                sub.canUseCv();
            case "API" ->
                sub.canUseApi();
            case "WATERMARK" ->
                !Boolean.TRUE.equals(plan.getWatermark()); // false watermark = no watermark
            case "DASHBOARD" ->
                Boolean.TRUE.equals(plan.getDashboard());
            default ->
                false;
        };
    }

    @Override
    @Transactional
    public void incrementUsage(Long userId, String feature) {
        Optional<UserSubscription> subscription = getActiveSubscription(userId);

        if (subscription.isEmpty()) {
            throw new BadRequestException("No active subscription found");
        }

        UserSubscription sub = subscription.get();

        switch (feature.toUpperCase()) {
            case "CV" -> {
                if (!sub.canUseCv()) {
                    throw new BadRequestException("CV limit reached");
                }
                sub.incrementCvCount();
            }
            case "API" -> {
                if (!sub.canUseApi()) {
                    throw new BadRequestException("API limit reached");
                }
                sub.incrementApiCount();
            }
            default ->
                throw new BadRequestException("Unknown feature: " + feature);
        }

        subscriptionRepository.save(sub);
    }

    @Override
    @Transactional
    public void activateSubscription(String paymentTransactionId) {
        log.info("Activating subscription for payment transaction {}", paymentTransactionId);

        // Find subscription by payment transaction ID
        UserSubscription subscription = subscriptionRepository.findByPaymentTransactionId(paymentTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found for payment transaction"));

        // Update subscription status
        subscription.setStatus(UserSubscription.SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Update user based on plan's dashboard feature
        User user = subscription.getUser();
        SubscriptionPlan plan = subscription.getPlan();

        // Only upgrade to licensed if plan has dashboard access
        if (plan.grantsLicensedStatus()) {
            user.setIsLicensed(true);
            user.setRole(Role.ROLE_LICENSED_USER);
        } else {
            // Keep user as ROLE_USER but with upgraded limits
            user.setRole(Role.ROLE_USER);
        }

        // Update user's plan name
        user.setPlan(plan.getName().toLowerCase());

        // Update limits based on plan
        if (plan.getCvLimit() != null) {
            user.setUiCvLimit(plan.getCvLimit());
        }
        if (plan.getApiLimit() != null) {
            user.setApiRateLimit(plan.getApiLimit());
        }

        // Update watermark setting based on plan
        user.setWatermark(plan.getWatermark());

        userRepository.save(user);

        log.info("Subscription activated successfully for user {} with plan {}", user.getId(), plan.getName());
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        log.info("Cancelling subscription {}", subscriptionId);

        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscription.setStatus(UserSubscription.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        // Downgrade user to free plan
        downgradeUserToFreePlan(subscription.getUser().getId());

        log.info("Subscription cancelled successfully");
    }

    @Override
    @Transactional
    public void renewSubscription(Long subscriptionId, boolean isAnnual) {
        log.info("Renewing subscription {}", subscriptionId);

        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        // Extend end date
        subscription.setEndDate(calculateEndDate(isAnnual));
        subscription.setStatus(UserSubscription.SubscriptionStatus.ACTIVE);

        // Reset usage counters
        subscription.setCurrentCvCount(0);
        subscription.setCurrentApiCount(0);

        subscriptionRepository.save(subscription);

        log.info("Subscription renewed successfully");
    }

    @Override
    @Transactional
    public void downgradeUserToFreePlan(Long userId) {
        log.info("Downgrading user {} to free plan", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(Role.ROLE_USER);
        user.setIsLicensed(false);
        user.setPlan("free");
        user.setUiCvLimit(5); // Free plan default
        user.setApiRateLimit(500); // Free plan default
        user.setWatermark(true); // Free plan has watermark
        userRepository.save(user);

        log.info("User {} downgraded to free plan successfully", userId);
    }

    private LocalDateTime calculateEndDate(boolean isAnnual) {
        return isAnnual
                ? LocalDateTime.now().plusYears(1)
                : LocalDateTime.now().plusMonths(1);
    }
}
