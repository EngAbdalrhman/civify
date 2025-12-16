package com.app.civify.service.subscription;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.entity.*;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.*;
import com.app.civify.security.model.Role;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Subscription service implementation with Option A flow: - User starts with
 * free plan (never expires) - When upgrading, free plan becomes inactive, paid
 * plan becomes active - When paid plan expires, free plan automatically
 * reactivates - Only ONE subscription is active at any time
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl extends com.app.civify.util.persistence.CommonService implements SubscriptionService {

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
    public PagedResponse<SubscriptionPlan> filterPlans(InputDataModel inputData) {
        log.debug("Filtering plans with criteria: {}", inputData);
        Page<SubscriptionPlan> page = filterEntities(planRepository, inputData);
        
        PagedResponse<SubscriptionPlan> response = new PagedResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        
        return response;
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
    public long countActiveSubscriptions() {
        return subscriptionRepository.count();
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
    public PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway) {
        log.info("Initiating payment for user {} with plan {} using gateway {}", userId, planId, gateway);

        // Create subscription first (with PENDING status)
        UserSubscription subscription = createSubscription(userId, planId, promoCode, isAnnual);

        // Calculate total price
        PriceCalculationResponse priceCalc = calculateTotalPrice(planId, isAnnual, promoCode);

        // Create payment transaction
        PaymentCreationResult paymentResult = paymentService.createPayment(userId, priceCalc.getTotalPrice().doubleValue(), gateway);

        // Link payment to subscription
        subscription.setPaymentTransactionId(paymentResult.getPaymentKey());
        subscriptionRepository.save(subscription);

        log.info("Payment initiated successfully for user {} with payment key {}", userId, paymentResult.getPaymentKey());

        return PaymentInitiationResponse.builder()
                .paymentKey(paymentResult.getPaymentKey())
                .paymentUrl(paymentResult.getPaymentUrl())
                .orderId(paymentResult.getOrderId())
                .subscriptionId(subscription.getId())
                .amount(priceCalc.getTotalPrice())
                .currency("EGP") // TODO detect via ip the nationality and currency egp or usd or sar or aed
                .message("Payment initiated successfully")
                .build();
    }

    @Override
    public boolean canUserUseFeature(Long userId, String feature) {
        Optional<UserSubscription> subscription = getActiveSubscription(userId);

        if (subscription.isEmpty()) {
            return false;
        }

        UserSubscription sub = subscription.get();
        SubscriptionPlan plan = sub.getPlan();
        User user = sub.getUser();

        return switch (feature.toUpperCase()) {
            case "CV" -> {
                // Check custom limit first
                if (user.getCvLimit() != null) {
                    sub.resetUsageIfNewMonth(); // Ensure usage is reset before checking
                    yield sub.getCurrentCvCount() < user.getCvLimit();
                }
                yield sub.canUseCv();
            }
            case "API" -> {
                // Check custom limit first
                if (user.getApiRateLimit() != null) {
                    sub.resetUsageIfNewMonth(); // Ensure usage is reset before checking
                    yield sub.getCurrentApiCount() < user.getApiRateLimit();
                }
                yield sub.canUseApi();
            }
            case "WATERMARK" ->
                !Boolean.TRUE.equals(plan.getWatermark()); // false = no watermark
            case "DASHBOARD" ->
                Boolean.TRUE.equals(plan.getDashboard());
            default ->
                false;
        };
    }

    @Override
    public boolean canUserUseTokens(Long userId, int tokensNeeded) {
        Optional<UserSubscription> subscription = getActiveSubscription(userId);
        if (subscription.isEmpty()) {
            return false;
        }
        UserSubscription sub = subscription.get();
        return sub.canUseTokens(Math.max(tokensNeeded, 0));
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
                if (!canUserUseFeature(userId, "CV")) {
                    throw new BadRequestException("CV generation limit reached for current subscription");
                }
                sub.incrementCvCount();
            }
            case "API" -> {
                if (!canUserUseFeature(userId, "API")) {
                    throw new BadRequestException("API call limit reached for current subscription");
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
    public void chargeTokens(Long userId, int tokensUsed) {
        Optional<UserSubscription> subscription = getActiveSubscription(userId);
        if (subscription.isEmpty()) {
            throw new BadRequestException("No active subscription found");
        }
        UserSubscription sub = subscription.get();
        if (!sub.canUseTokens(tokensUsed)) {
            throw new BadRequestException("Token allowance exceeded for current subscription");
        }
        sub.incrementTokenUsage(tokensUsed);
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
     * Calculate end date based on plan and billing cycle Free plans never
     * expire (100 years)
     */
    private LocalDateTime calculateEndDate(SubscriptionPlan plan, boolean isAnnual) {
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            return LocalDateTime.now().plusYears(100); // Effectively permanent
        }

        return isAnnual
                ? LocalDateTime.now().plusYears(1)
                : LocalDateTime.now().plusMonths(1);
    }

    // ==================== ADMIN PLAN MANAGEMENT ====================
    @Override
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        log.info("Creating new subscription plan: {}", plan.getName());

        // Validate plan name is unique
        if (planRepository.findByName(plan.getName()).isPresent()) {
            throw new BadRequestException("Plan with name '" + plan.getName() + "' already exists");
        }

        // Validate pricing
        if (plan.getMonthlyPrice() != null && plan.getMonthlyPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Monthly price cannot be negative");
        }
        if (plan.getAnnualPrice() != null && plan.getAnnualPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Annual price cannot be negative");
        }

        // Set defaults
        if (plan.getActive() == null) {
            plan.setActive(true);
        }
        if (plan.getWatermark() == null) {
            plan.setWatermark(true);
        }
        if (plan.getDashboard() == null) {
            plan.setDashboard(false);
        }

        SubscriptionPlan saved = planRepository.save(plan);
        log.info("Subscription plan created successfully with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public SubscriptionPlan updatePlan(Long planId, SubscriptionPlan updatedPlan) {
        log.info("Updating subscription plan: {}", planId);

        SubscriptionPlan existingPlan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        // Get count of active subscriptions for this plan
        long activeSubscriptionCount = getActiveSubscriptionCountForPlan(planId);

        // If there are active subscriptions, validate that critical features aren't being reduced
        if (activeSubscriptionCount > 0) {
            validatePlanUpdate(existingPlan, updatedPlan);
        }

        // Update allowed fields
        if (updatedPlan.getName() != null && !updatedPlan.getName().equals(existingPlan.getName())) {
            // Check if new name is unique
            if (planRepository.findByName(updatedPlan.getName()).isPresent()) {
                throw new BadRequestException("Plan with name '" + updatedPlan.getName() + "' already exists");
            }
            existingPlan.setName(updatedPlan.getName());
        }

        if (updatedPlan.getDescription() != null) {
            existingPlan.setDescription(updatedPlan.getDescription());
        }

        if (updatedPlan.getDetails() != null) {
            existingPlan.setDetails(updatedPlan.getDetails());
        }

        if (updatedPlan.getCvLimit() != null) {
            existingPlan.setCvLimit(updatedPlan.getCvLimit());
        }

        if (updatedPlan.getApiLimit() != null) {
            existingPlan.setApiLimit(updatedPlan.getApiLimit());
        }

        if (updatedPlan.getWatermark() != null) {
            existingPlan.setWatermark(updatedPlan.getWatermark());
        }

        if (updatedPlan.getDashboard() != null) {
            existingPlan.setDashboard(updatedPlan.getDashboard());
        }

        if (updatedPlan.getMonthlyPrice() != null) {
            if (updatedPlan.getMonthlyPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Monthly price cannot be negative");
            }
            existingPlan.setMonthlyPrice(updatedPlan.getMonthlyPrice());
        }

        if (updatedPlan.getAnnualPrice() != null) {
            if (updatedPlan.getAnnualPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Annual price cannot be negative");
            }
            existingPlan.setAnnualPrice(updatedPlan.getAnnualPrice());
        }

        SubscriptionPlan saved = planRepository.save(existingPlan);
        log.info("Subscription plan updated successfully: {}", planId);
        return saved;
    }

    @Override
    @Transactional
    public SubscriptionPlan activatePlan(Long planId) {
        log.info("Activating subscription plan: {}", planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        plan.setActive(true);
        SubscriptionPlan saved = planRepository.save(plan);
        log.info("Subscription plan activated successfully: {}", planId);
        return saved;
    }

    @Override
    @Transactional
    public SubscriptionPlan deactivatePlan(Long planId) {
        log.info("Deactivating subscription plan: {}", planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        // Prevent deactivating the free plan
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            throw new BadRequestException("Cannot deactivate the free plan");
        }

        plan.setActive(false);
        SubscriptionPlan saved = planRepository.save(plan);
        log.info("Subscription plan deactivated successfully: {}. Existing subscriptions remain active.", planId);
        return saved;
    }

    @Override
    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Override
    public long getActiveSubscriptionCountForPlan(Long planId) {
        return subscriptionRepository.countActiveSubscriptionsByPlanId(planId);
    }

    @Override
    @Transactional
    public void deletePlan(Long planId) {
        log.info("Deleting subscription plan: {}", planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        // Prevent deleting the free plan
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            throw new BadRequestException("Cannot delete the free plan");
        }

        // Check if there are any active subscriptions
        long activeSubscriptionCount = getActiveSubscriptionCountForPlan(planId);
        if (activeSubscriptionCount > 0) {
            throw new BadRequestException(
                    "Cannot delete plan with active subscriptions. Please deactivate the plan instead.");
        }

        // Soft delete
        plan.setIsDeleted(true);
        planRepository.save(plan);
        log.info("Subscription plan soft deleted successfully: {}", planId);
    }

    /**
     * Validate that plan updates don't break existing active subscriptions
     */
    private void validatePlanUpdate(SubscriptionPlan existingPlan, SubscriptionPlan updatedPlan) {
        // Prevent reducing limits if they were previously set
        if (existingPlan.getCvLimit() != null && updatedPlan.getCvLimit() != null) {
            if (updatedPlan.getCvLimit() < existingPlan.getCvLimit()) {
                log.warn("Attempting to reduce CV limit from {} to {} for plan with active subscriptions",
                        existingPlan.getCvLimit(), updatedPlan.getCvLimit());
                throw new BadRequestException(
                        "Cannot reduce CV limit for a plan with active subscriptions. Current: "
                        + existingPlan.getCvLimit() + ", Requested: " + updatedPlan.getCvLimit());
            }
        }

        if (existingPlan.getApiLimit() != null && updatedPlan.getApiLimit() != null) {
            if (updatedPlan.getApiLimit() < existingPlan.getApiLimit()) {
                log.warn("Attempting to reduce API limit from {} to {} for plan with active subscriptions",
                        existingPlan.getApiLimit(), updatedPlan.getApiLimit());
                throw new BadRequestException(
                        "Cannot reduce API limit for a plan with active subscriptions. Current: "
                        + existingPlan.getApiLimit() + ", Requested: " + updatedPlan.getApiLimit());
            }
        }

        // Prevent removing features that were previously enabled
        if (Boolean.FALSE.equals(existingPlan.getWatermark()) && Boolean.TRUE.equals(updatedPlan.getWatermark())) {
            throw new BadRequestException(
                    "Cannot add watermark to a plan that previously had it removed (active subscriptions exist)");
        }

        if (Boolean.TRUE.equals(existingPlan.getDashboard()) && Boolean.FALSE.equals(updatedPlan.getDashboard())) {
            throw new BadRequestException(
                    "Cannot remove dashboard access from a plan that previously had it (active subscriptions exist)");
        }
    }
}
