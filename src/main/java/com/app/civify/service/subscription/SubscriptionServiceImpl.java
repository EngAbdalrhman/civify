package com.app.civify.service.subscription;

import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.entity.*;
import com.app.civify.entity.enums.SubscriptionStatus;
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

/**
 * Subscription service implementation with Option A flow:
 * - User starts with free plan (never expires)
 * - When upgrading, free plan becomes inactive, paid plan becomes active
 * - When paid plan expires, free plan automatically reactivates
 * - Only ONE subscription is active at any time
 */
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

    private static final String FREE_PLAN_NAME = "free";

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

        // Don't allow creating another subscription if user already has a pending one
        Optional<UserSubscription> pendingSubscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.PENDING)
                .stream()
                .findFirst();

        if (pendingSubscription.isPresent()) {
            throw new BadRequestException("User already has a pending subscription. Please complete or cancel it first.");
        }

        // For free plan, check if user already has it
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            Optional<UserSubscription> existingFree = subscriptionRepository
                    .findByUserIdAndPlanName(userId, FREE_PLAN_NAME);
            if (existingFree.isPresent()) {
                throw new BadRequestException("User already has a free plan");
            }
        }

        // Create new subscription with PENDING status (will be activated after payment)
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(calculateEndDate(plan, isAnnual));
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setCurrentCvCount(0);
        subscription.setCurrentApiCount(0);

        // Set usage reset date to first day of next month
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        subscription.setUsageResetDate(nextMonth);

        // Apply promo code if provided
        if (promoCode != null && !promoCode.isEmpty()) {
            PromoCode promo = promoCodeRepository.findByCodeAndActiveTrue(promoCode)
                    .orElseThrow(() -> new BadRequestException("Invalid or expired promo code"));

            if (!promo.isValid()) {
                throw new BadRequestException("Promo code is not valid");
            }

            subscription.setPromoCode(promo);

            // Increment promo code usage
            promo.setUsedCount(promo.getUsedCount() + 1);
            promoCodeRepository.save(promo);
        }

        UserSubscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created with ID {} for user {}", saved.getId(), userId);

        return saved;
    }

    @Override
    public Optional<UserSubscription> getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(UserSubscription::isActive)
                .findFirst();
    }

    @Override
    public List<UserSubscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    @Override
    public PriceCalculationResponse calculateTotalPrice(Long planId, boolean isAnnual, String promoCode) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        BigDecimal basePrice = isAnnual ? plan.getAnnualPrice() : plan.getMonthlyPrice();
        BigDecimal discount = BigDecimal.ZERO;
        String appliedPromoCode = null;

        // Apply promo code discount if provided
        if (promoCode != null && !promoCode.isEmpty()) {
            Optional<PromoCode> promo = promoCodeRepository.findByCodeAndActiveTrue(promoCode);
            if (promo.isPresent() && promo.get().isValid()) {
                discount = promo.get().calculateDiscount(basePrice);
                appliedPromoCode = promoCode;
            }
        }

        BigDecimal totalPrice = basePrice.subtract(discount);

        return PriceCalculationResponse.builder()
                .planId(planId)
                .planName(plan.getName())
                .basePrice(basePrice)
                .discount(discount)
                .totalPrice(totalPrice)
                .promoCode(appliedPromoCode)
                .isAnnual(isAnnual)
                .build();
    }

    @Override
    @Transactional
    public String initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode) {
        log.info("Initiating payment for user {} with plan {}", userId, planId);

        // Create subscription first (with PENDING status)
        UserSubscription subscription = createSubscription(userId, planId, promoCode, isAnnual);

        // Calculate total price
        PriceCalculationResponse priceCalc = calculateTotalPrice(planId, isAnnual, promoCode);

        // Create payment transaction
        String paymentKey = paymentService.createPayment(userId, priceCalc.getTotalPrice().doubleValue());

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
            case "CV" -> sub.canUseCv();
            case "API" -> sub.canUseApi();
            case "WATERMARK" -> !Boolean.TRUE.equals(plan.getWatermark()); // false = no watermark
            case "DASHBOARD" -> Boolean.TRUE.equals(plan.getDashboard());
            default -> false;
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
                    throw new BadRequestException("CV generation limit reached for current subscription");
                }
                sub.incrementCvCount();
            }
            case "API" -> {
                if (!sub.canUseApi()) {
                    throw new BadRequestException("API call limit reached for current subscription");
                }
                sub.incrementApiCount();
            }
            default -> throw new BadRequestException("Unknown feature: " + feature);
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

        User user = subscription.getUser();
        SubscriptionPlan plan = subscription.getPlan();

        // OPTION A FLOW: Deactivate other subscriptions (including free plan)
        List<UserSubscription> activeSubscriptions = subscriptionRepository
                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);

        for (UserSubscription activeSub : activeSubscriptions) {
            activeSub.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(activeSub);
            log.info("Deactivated previous subscription {} for user {}", activeSub.getId(), user.getId());
        }

        // Activate the new subscription
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Update user role if plan grants licensed status
        if (plan.grantsLicensedStatus() && user.getRole() != Role.ROLE_ADMIN) {
            user.setRole(Role.ROLE_LICENSED_USER);
            userRepository.save(user);
        }

        log.info("Subscription activated successfully for user {} with plan {}", user.getId(), plan.getName());
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        log.info("Cancelling subscription {}", subscriptionId);

        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        // Reactivate free plan if it exists
        reactivateFreePlan(subscription.getUser().getId());

        log.info("Subscription cancelled successfully");
    }

    @Override
    @Transactional
    public void renewSubscription(Long subscriptionId, boolean isAnnual) {
        log.info("Renewing subscription {}", subscriptionId);

        UserSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        // Extend end date from current end date (not from now)
        LocalDateTime newEndDate = subscription.getEndDate().isAfter(LocalDateTime.now())
                ? subscription.getEndDate()
                : LocalDateTime.now();

        newEndDate = isAnnual ? newEndDate.plusYears(1) : newEndDate.plusMonths(1);

        subscription.setEndDate(newEndDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);

        log.info("Subscription renewed successfully until {}", newEndDate);
    }

    @Override
    @Transactional
    public void createDefaultFreeSubscription(Long userId) {
        log.info("Creating default free subscription for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user already has a free subscription
        Optional<UserSubscription> existingFree = subscriptionRepository
                .findByUserIdAndPlanName(userId, FREE_PLAN_NAME);

        if (existingFree.isPresent()) {
            log.info("User {} already has a free subscription", userId);
            return;
        }

        // Get the free plan
        SubscriptionPlan freePlan = planRepository.findByName(FREE_PLAN_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Free plan not found. Please create a subscription plan named 'free' in the database."));

        // Create permanent free subscription (never expires)
        UserSubscription freeSubscription = new UserSubscription();
        freeSubscription.setUser(user);
        freeSubscription.setPlan(freePlan);
        freeSubscription.setStartDate(LocalDateTime.now());
        freeSubscription.setEndDate(LocalDateTime.now().plusYears(100)); // Effectively permanent
        freeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        freeSubscription.setCurrentCvCount(0);
        freeSubscription.setCurrentApiCount(0);

        // Set usage reset date
        LocalDateTime nextMonth = LocalDateTime.now().plusMonths(1).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        freeSubscription.setUsageResetDate(nextMonth);

        subscriptionRepository.save(freeSubscription);

        log.info("Default free subscription created for user {}", userId);
    }

    @Override
    @Transactional
    public void reactivateFreePlan(Long userId) {
        log.info("Reactivating free plan for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find user's free plan subscription
        Optional<UserSubscription> freeSub = subscriptionRepository
                .findByUserIdAndPlanName(userId, FREE_PLAN_NAME);

        if (freeSub.isPresent()) {
            UserSubscription freeSubscription = freeSub.get();
            freeSubscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(freeSubscription);
            log.info("Free plan reactivated for user {}", userId);
        } else {
            // If no free plan exists, create one
            log.warn("No free plan found for user {}, creating new one", userId);
            createDefaultFreeSubscription(userId);
        }

        // Downgrade user role if they were licensed
        if (user.getRole() == Role.ROLE_LICENSED_USER) {
            user.setRole(Role.ROLE_USER);
            userRepository.save(user);
            log.info("User {} downgraded to regular user role", userId);
        }
    }

    /**
     * Calculate end date based on plan and billing cycle
     * Free plans never expire (100 years)
     */
    private LocalDateTime calculateEndDate(SubscriptionPlan plan, boolean isAnnual) {
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            return LocalDateTime.now().plusYears(100); // Effectively permanent
        }

        return isAnnual
                ? LocalDateTime.now().plusYears(1)
                : LocalDateTime.now().plusMonths(1);
    }
}
