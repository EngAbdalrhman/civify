package com.app.civify.service.subscription;

import com.app.civify.entity.enums.TransactionType;

import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.PromoApplicabilityType;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.entity.plans.*;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.UserRepository;
import com.app.civify.repository.plans.PromoCodeRepository;
import com.app.civify.repository.plans.SubscriptionPlanRepository;
import com.app.civify.repository.plans.UserPromoUsageRepository;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.service.logs.TokenUsageService;
import com.app.civify.service.logs.SubscriptionAuditService;
import com.app.civify.entity.enums.SubscriptionAuditType;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import com.app.civify.repository.billing.BillingTransactionRepository;
import com.app.civify.service.i18n.MessageService;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class SubscriptionServiceImpl extends CommonService implements SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final UserPromoUsageRepository userPromoUsageRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final MessageService messageService;
    private final BillingTransactionRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final TokenUsageService tokenUsageService;
    private final EmailService emailService;
    private final SubscriptionAuditService auditService;

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
                .orElseThrow(() -> new ResourceNotFoundException("user.notfound"));
        return createSubscription(user, planId, promoCode, isAnnual);
    }

    @Override
    @Transactional
    public UserSubscription createSubscription(User user, Long planId, String promoCode, boolean isAnnual) {
        Long userId = user.getId();
        log.info("Creating subscription for user {} with plan {}", userId, planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

        // ==================== BILLING RESTRICTIONS ====================
        // Check if annual billing is allowed for this plan
        if (isAnnual && Boolean.FALSE.equals(plan.getAllowAnnualBilling())) {
            throw new BadRequestException("subscription.annual.notallowed");
        }

        // Check if renewal is allowed (for returning users)
        Optional<UserSubscription> previousSub = subscriptionRepository
                .findByUserIdAndPlanIdAndStatus(userId, planId, SubscriptionStatus.EXPIRED);
        if (previousSub.isPresent() && Boolean.FALSE.equals(plan.getRenewalAllowed())) {
            throw new BadRequestException("subscription.renewal.notallowed");
        }
        // =============================================================

        Optional<UserSubscription> pendingSubscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.PENDING)
                .stream()
                .findFirst();

        if (pendingSubscription.isPresent()) {
            UserSubscription pending = pendingSubscription.get();
            // Check if it's stale (older than 5 minutes)
            if (pending.getCreatedAt() != null && 
                pending.getCreatedAt().isBefore(LocalDateTime.now().minusSeconds(10))) {
                
                log.info("Found stale pending subscription {} created at {}. Marking as CANCELLED.", 
                        pending.getId(), pending.getCreatedAt());
                
                pending.setStatus(SubscriptionStatus.CANCELLED);
                subscriptionRepository.save(pending);
                // Allow proceeding to create new one
            } else {
                throw new BadRequestException("subscription.pending.exists");
            }
        }

        // Block same-plan subscription (user already has active subscription for THIS plan)
        Optional<UserSubscription> activeSamePlan = subscriptionRepository
                .findByUserIdAndPlanIdAndStatus(userId, planId, SubscriptionStatus.ACTIVE);
        if (activeSamePlan.isPresent()) {
            throw new BadRequestException("subscription.active.exists");
        }

        // For free plan, check if user already has it
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            Optional<UserSubscription> existingFree = subscriptionRepository
                    .findByUserIdAndPlanName(userId, FREE_PLAN_NAME);
            if (existingFree.isPresent()) {
                throw new BadRequestException("subscription.free.exists");
            }
        }

        // Check if this is an upgrade (user has different active plan)
        Optional<UserSubscription> currentActiveSub = getActiveSubscription(userId);
        LocalDateTime subscriptionStartDate = LocalDateTime.now();
        
        if (currentActiveSub.isPresent() && !currentActiveSub.get().getPlan().getId().equals(planId)) {
            // Check if current plan is NOT free (Free plans never expire, so we shouldn't queue after them)
            boolean isFreePlan = FREE_PLAN_NAME.equalsIgnoreCase(currentActiveSub.get().getPlan().getName());
            
            if (!isFreePlan) {
                // Queue upgrade: new subscription starts when current one ends
                subscriptionStartDate = currentActiveSub.get().getEndDate();
                // Disable auto-renew on current subscription
                UserSubscription current = currentActiveSub.get();
                current.setAutoRenew(false);
                subscriptionRepository.save(current);
                log.info("Queued upgrade for user {}. New plan starts after current expires on {}", userId, subscriptionStartDate);
            }
        }

        // Create new subscription with PENDING status (will be activated after payment)
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(subscriptionStartDate);
        subscription.setEndDate(calculateEndDate(plan, isAnnual, subscriptionStartDate));
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setCurrentCvCount(0);
        subscription.setCurrentCycleCount(1);

        // Set usage reset date to first day of next month from start date
        LocalDateTime nextMonth = subscriptionStartDate.plusMonths(1).withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        subscription.setUsageResetDate(nextMonth);

        // Validate and apply promo code if provided
        if (promoCode != null && !promoCode.isEmpty()) {
            PromoCode promo = validatePromoCode(userId, planId, promoCode);
            subscription.setPromoCode(promo);
        }

        UserSubscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created with ID {} for user {}", saved.getId(), userId);

        return saved;
    }

    /**
     * Validate promo code without recording usage. Returns the promo if valid.
     */
    private PromoCode validatePromoCode(Long userId, Long planId, String promoCode) {
        PromoCode promo = promoCodeRepository.findByCodeAndActiveTrue(promoCode)
                .orElseThrow(() -> new BadRequestException("promo.invalid"));

        if (!promo.isValid()) {
            throw new BadRequestException("promo.nolongervalid");
        }

        // Check if promo is applicable to plans (not token-only)
        if (promo.getApplicabilityType() == PromoApplicabilityType.TOKEN) {
            throw new BadRequestException("promo.tokenonly");
        }

        // Check if promo applies to this specific plan
        if (!promo.isApplicableToPlan(planId)) {
            throw new BadRequestException("promo.notforplan");
        }

        // Check if user has already used this promo
        if (userPromoUsageRepository.existsByUserIdAndPromoCodeId(userId, promo.getId())) {
            throw new BadRequestException("promo.alreadyused");
        }

        return promo;
    }

    private void recordPromoUsage(Long userId, PromoCode promo) {
        // Re-check to avoid double usage if multiple pending subs were created
        if (userPromoUsageRepository.existsByUserIdAndPromoCodeId(userId, promo.getId())) {
            log.warn("Promo code {} already recorded for user {}. Skipping duplicate recording.", promo.getCode(), userId);
            return;
        }

        // Record promo usage for this user
        UserPromoUsage usage = UserPromoUsage.builder()
                .user(userRepository.getReferenceById(userId))
                .promoCode(promo)
                .usedAt(LocalDateTime.now())
                .build();
        userPromoUsageRepository.save(usage);

        // Increment global promo usage count
        promo.setUsedCount(promo.getUsedCount() + 1);
        promoCodeRepository.save(promo);
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
        return subscriptionRepository.countByStatusAndPlanNameNotIgnoreCase(
            SubscriptionStatus.ACTIVE, 
            FREE_PLAN_NAME
        );
    }

    @Override
    public PriceCalculationResponse calculateTotalPrice(Long userId, Long planId, boolean isAnnual, String promoCode) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

        BigDecimal basePrice = isAnnual ? plan.getAnnualPrice() : plan.getMonthlyPrice();
        BigDecimal discount = BigDecimal.ZERO;
        String appliedPromoCode = null;

        // Apply promo code discount if provided
        if (promoCode != null && !promoCode.isEmpty()) {
            Optional<PromoCode> promoOpt = promoCodeRepository.findByCodeAndActiveTrue(promoCode);
            if (promoOpt.isPresent()) {
                PromoCode promo = promoOpt.get();
                if (!promo.isValid()) {
                    // Invalid
                } else if (promo.getApplicabilityType() == PromoApplicabilityType.TOKEN) {
                    // Not for plan
                } else if (!promo.isApplicableToPlan(planId)) {
                    // Not for this plan
                } else if (userId != null && userPromoUsageRepository.existsByUserIdAndPromoCodeId(userId, promo.getId())) {
                    // Already used
                } else {
                    discount = promo.calculateDiscount(basePrice);
                    appliedPromoCode = promoCode;
                }
            } else {
                // Invalid code
            }
        }

        BigDecimal totalPrice = basePrice
                .subtract(discount)
                .max(BigDecimal.ZERO);

        return PriceCalculationResponse.builder()
                .planId(planId)
                .planName(plan.getName())
                .basePrice(basePrice)
                .discount(discount)
                .totalPrice(totalPrice)
                .promoCode(appliedPromoCode)
                .isAnnual(isAnnual)
                .billingCycle(isAnnual ? "ANNUAL" : "MONTHLY")
                .currency("EGP")
                .build();
    }

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, PaymentMethods method, String phoneNumber) {
        log.info("Initiating payment for user {} with plan {} using gateway {}", userId, planId, gateway);

        // Calculate total price first (before marking promo as used)
        PriceCalculationResponse priceCalc = calculateTotalPrice(userId, planId, isAnnual, promoCode);

        // Create subscription (with PENDING status) - this marks promo as used
        UserSubscription subscription = createSubscription(userId, planId, promoCode, isAnnual);

        // Check if price is zero (Free plan or 100% discount)
        if (priceCalc.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Total price is 0. Bypassing payment gateway for user {}", userId);
            String transactionId = "FREE-" + java.util.UUID.randomUUID();
            
            subscription.setPaymentTransactionId(transactionId);
            subscriptionRepository.save(subscription);
            
            activateSubscription(transactionId);
            
            return PaymentInitiationResponse.builder()
                    .paymentKey(transactionId)
                    .paymentUrl("/app/") // Redirect to dashboard
                    .orderId(transactionId)
                    .subscriptionId(subscription.getId())
                    .amount(BigDecimal.ZERO)
                    .currency("EGP")
                    .message(messageService.getMessage("subscription.created"))
                    .build();
        }

        // Create payment transaction
        Map<String, Object> metadata = new HashMap<>();
        if (subscription.getUser() != null) {
            User user = subscription.getUser();
            metadata.put("firstName", user.getFirstName());
            metadata.put("lastName", user.getLastName());
            metadata.put("email", user.getEmail());
            // Prioritize passed phone number (e.g. for wallet payment), otherwise use user profile phone
            String phoneToUse = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber :
                               (user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getPhone());
            metadata.put("phone", phoneToUse);
            metadata.put("subscriptionId", subscription.getId());
        }

        PaymentCreationResult paymentResult = paymentService.createPayment(userId, priceCalc.getTotalPrice().doubleValue(), gateway, method, TransactionType.SUBSCRIPTION, metadata);

        // Link payment to subscription using orderId (same ID used for lookup in activateSubscription)
        subscription.setPaymentTransactionId(paymentResult.getOrderId());
        subscriptionRepository.save(subscription);

        // Link BillingTransaction → Subscription (needed for token extraction in webhook)
        paymentRepository.findByOrderId(paymentResult.getOrderId()).ifPresent(tx -> {
            tx.setSubscriptionId(subscription.getId());
            paymentRepository.save(tx);
        });

        log.info("Payment initiated successfully for user {} with payment key {}", userId, paymentResult.getPaymentKey());

        return PaymentInitiationResponse.builder()
                .paymentKey(paymentResult.getPaymentKey())
                .paymentUrl(paymentResult.getPaymentUrl())
                .orderId(paymentResult.getOrderId())
                .subscriptionId(subscription.getId())
                .amount(priceCalc.getTotalPrice())
                .currency("EGP") 
                .message(messageService.getMessage("payment.initiated"))
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
                    yield sub.getCurrentCvCount() < user.getCvLimit();
                }
                yield sub.canUseCv();
            }
            case "JOB_MATCH" -> {
                if (user.getJobMatchLimit() != null) {
                    yield sub.getCurrentJobMatchCount() < user.getJobMatchLimit();
                }
                yield sub.canUseJobMatch();
            }
            case "WATERMARK" ->
                !Boolean.TRUE.equals(plan.getWatermark()); // false = no watermark
            default ->
                false;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserUseTokens(Long userId, int tokensNeeded) {
        // Efficiently fetch token info for active subscription
        var tokenInfoOpt = subscriptionRepository.findTokenInfoByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        
        if (tokenInfoOpt.isEmpty()) {
            int walletTokens = userRepository.findExtraTokensByUserId(userId).orElse(0);
            return walletTokens >= tokensNeeded;
        }
        
        var info = tokenInfoOpt.get();
        // Check local monthly limit
        int currentUsage = info.getCurrentTokenUsage() != null ? info.getCurrentTokenUsage() : 0;
        int limit = info.getTokenLimit() != null ? info.getTokenLimit() : Integer.MAX_VALUE;
        int monthlyRemaining = Math.max(0, limit - currentUsage);

        if (monthlyRemaining >= tokensNeeded) {
            return true;
        }

        // Fetch wallet tokens efficiently
        int walletTokens = userRepository.findExtraTokensByUserId(userId).orElse(0); 
        return ((long) monthlyRemaining + walletTokens) >= tokensNeeded;
    }

    @Override
    @Transactional
    public void incrementUsage(Long userId, String feature) {
        Optional<UserSubscription> subscription = getActiveSubscription(userId);

        if (subscription.isEmpty()) {
            throw new BadRequestException("subscription.notfound");
        }

        UserSubscription sub = subscription.get();

        switch (feature.toUpperCase()) {
            case "CV" -> {
                if (!canUserUseFeature(userId, "CV")) {
                    throw new BadRequestException("subscription.cv.limit.reached");
                }
                sub.incrementCvCount();
            }
            case "JOB_MATCH" -> {
                if (!canUserUseFeature(userId, "JOB_MATCH")) {
                    throw new BadRequestException("subscription.jobmatch.limit.reached");
                }
                sub.incrementJobMatchCount();
            }
            default ->
                throw new BadRequestException(messageService.getMessage("error.feature.unknown") + ": " + feature);
        }

        subscriptionRepository.save(sub);
    }

    @Override
    @Transactional
    public void chargeTokens(Long userId, int tokensUsed) {
        // Delegate to overloaded method with default usage type
        chargeTokens(userId, tokensUsed, TokenUsageType.OTHER, null);
    }

    @Override
    @Transactional
    public void chargeTokens(Long userId, int tokensUsed, TokenUsageType usageType, String description) {
        // Optimized charge logic using projections and direct updates
        var tokenInfoOpt = subscriptionRepository.findTokenInfoByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        
        int fromSubscription = 0;
        int fromWallet = 0;
        
        if (tokenInfoOpt.isEmpty()) {
            // No active subscription - charge from wallet only
            int walletTokens = userRepository.findExtraTokensByUserId(userId).orElse(0);
            
            userRepository.updateExtraTokens(userId, Math.max(0, walletTokens - tokensUsed));
            fromWallet = tokensUsed;
            log.info("Charged {} tokens from wallet for user {} (no active subscription). Balance floored at 0.", tokensUsed, userId);
        } else {
            var info = tokenInfoOpt.get();
            int currentUsage = info.getCurrentTokenUsage() != null ? info.getCurrentTokenUsage() : 0;
            int limit = info.getTokenLimit() != null ? info.getTokenLimit() : Integer.MAX_VALUE;
            int monthlyRemaining = Math.max(0, limit - currentUsage);
            
            int deductFromMonthly = Math.min(tokensUsed, monthlyRemaining);
            int remainingToCharge = tokensUsed - deductFromMonthly;
            
            // 1. Deduct from monthly allowance (subscription usage)
            if (deductFromMonthly > 0) {
                subscriptionRepository.updateTokenUsage(info.getId(), currentUsage + deductFromMonthly);
                fromSubscription = deductFromMonthly;
            }
            
            // 2. Deduct remaining from user's wallet (extra tokens)
            if (remainingToCharge > 0) {
                int walletTokens = userRepository.findExtraTokensByUserId(userId).orElse(0);
                
                // Allow negative balance (deficit) -> Requested change: min 0
                userRepository.updateExtraTokens(userId, Math.max(0, walletTokens - remainingToCharge));
                fromWallet = remainingToCharge;
            }
        }
        
        // Log the token usage
        tokenUsageService.logUsage(userId, usageType, tokensUsed, fromSubscription, fromWallet, description);
    }

    @Override
    @Transactional
    public void activateSubscription(String paymentTransactionId) {
        log.info("Activating subscription for payment transaction {}", paymentTransactionId);

        // Find subscription by payment transaction ID
        UserSubscription subscription = subscriptionRepository.findByPaymentTransactionId(paymentTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("subscription.notfound"));

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

        // Record promo usage if applicable
        if (subscription.getPromoCode() != null) {
            recordPromoUsage(user.getId(), subscription.getPromoCode());
        }

        // Update user role if plan grants licensed status
        // Update user group based on plan
        if (plan.getLinkedGroup() != null && !Boolean.TRUE.equals(user.getGroup().getIsSystemGroup())) {
            user.setGroup(plan.getLinkedGroup());
            userRepository.save(user);
        }

        log.info("Subscription activated successfully for user {} with plan {}", user.getId(), plan.getName());

        BigDecimal amountPaid = BigDecimal.ZERO;
        if (paymentTransactionId != null && !paymentTransactionId.startsWith("FREE-")) {
            java.util.Optional<com.app.civify.entity.billing.BillingTransaction> txOpt = paymentRepository.findByOrderId(paymentTransactionId);
            if (txOpt.isPresent() && txOpt.get().getAmount() != null) {
                amountPaid = txOpt.get().getAmount();
            }
        }

        // Send subscription activation thank-you email
        emailService.sendSubscriptionActivationEmail(user, plan, subscription, amountPaid);
        
        // Log audit event for compliance
        auditService.logEvent(
            user.getId(), 
            subscription.getId(), 
            SubscriptionAuditType.ACTIVATED, 
            null, // Invoice ID not available here directly, would need lookup if critical
            plan.getId(), 
            "SYSTEM", 
            "Subscription activated via payment: " + paymentTransactionId
        );
    }

    @Override
    @Transactional
    public void cancelActiveSubscription(Long userId) {
        log.info("Cancelling active subscription for user {}", userId);

        // Find active non-free subscription
        UserSubscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(sub -> !FREE_PLAN_NAME.equalsIgnoreCase(sub.getPlan().getName()))
                .filter(sub -> !FREE_PLAN_NAME.equalsIgnoreCase(sub.getPlan().getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("subscription.nonfree.notfound"));

        // Mark subscription to not auto-renew (graceful cancellation)
        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        // Cancel any pending invoices for this subscription
        invoiceService.cancelPendingInvoices(subscription.getId());

        log.info("Subscription {} marked for cancellation (autoRenew=false). Will expire on {}", 
                subscription.getId(), subscription.getEndDate());
                
        // Log audit event for compliance
        auditService.logEvent(
            userId, 
            subscription.getId(), 
            SubscriptionAuditType.CANCELLED, 
            null, 
            subscription.getPlan().getId(), 
            "USER", 
            "User requested cancellation of auto-renewal"
        );
    }

    @Override
    @Transactional
    public void createDefaultFreeSubscription(Long userId) {
        log.info("Creating default free subscription for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user.notfound"));

        // Check if user already has a free subscription
        Optional<UserSubscription> existingFree = subscriptionRepository
                .findByUserIdAndPlanName(userId, FREE_PLAN_NAME);

        if (existingFree.isPresent()) {
            log.info("User {} already has a free subscription", userId);
            return;
        }

        // Get the free plan
        SubscriptionPlan freePlan = planRepository.findByName(FREE_PLAN_NAME)
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

        // Create permanent free subscription (never expires)
        UserSubscription freeSubscription = new UserSubscription();
        freeSubscription.setUser(user);
        freeSubscription.setPlan(freePlan);
        freeSubscription.setStartDate(LocalDateTime.now());
        freeSubscription.setEndDate(LocalDateTime.now().plusYears(100)); // Effectively permanent
        freeSubscription.setStatus(SubscriptionStatus.ACTIVE);
        freeSubscription.setCurrentCvCount(0);

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
                .orElseThrow(() -> new ResourceNotFoundException("user.notfound"));

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

        // Downgrade user group to Free Group (if they are not a System User e.g. Admin)
        if (!Boolean.TRUE.equals(user.getGroup().getIsSystemGroup())) {
             // We need to fetch the free plan first to get its group, which we did above or need to fetch again if not present
             // The logic above either found `freeSub` (which has `freePlan` -> `freeGroup`) or `createDefaultFreeSubscription` (which doesn't return plan/group here immediately).
             // However, `createDefaultFreeSubscription` saves the sub, but doesn't update the user's group field per se unless we do it here.
             
             // Simplest approach: Fetch Free Plan again to be sure
             Optional<SubscriptionPlan> freePlanOpt = planRepository.findByName(FREE_PLAN_NAME);
             if (freePlanOpt.isPresent() && freePlanOpt.get().getLinkedGroup() != null) {
                 user.setGroup(freePlanOpt.get().getLinkedGroup());
                 userRepository.save(user);
                 log.info("User {} downgraded to free group", userId);
             }
        }
    }



    /**
     * Calculate end date from a specific start date.
     */
    private LocalDateTime calculateEndDate(SubscriptionPlan plan, boolean isAnnual, LocalDateTime startDate) {
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            return startDate.plusYears(100); // Effectively permanent
        }

        return isAnnual
                ? startDate.plusYears(1)
                : startDate.plusMonths(1);
    }

    // ==================== ADMIN PLAN MANAGEMENT ====================
    @Override
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        log.info("Creating new subscription plan: {}", plan.getName());

        // Validate plan name is unique
        if (planRepository.findByName(plan.getName()).isPresent()) {
            throw new BadRequestException(messageService.getMessage("subscription.plan.name.exists") + ": " + plan.getName());
        }

        // Validate pricing
        if (plan.getMonthlyPrice() != null && plan.getMonthlyPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("validation.price.negative");
        }
        if (plan.getAnnualPrice() != null && plan.getAnnualPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("validation.price.negative");
        }

        // Set defaults
        if (plan.getActive() == null) {
            plan.setActive(true);
        }
        if (plan.getWatermark() == null) {
            plan.setWatermark(true);
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
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

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
                throw new BadRequestException(messageService.getMessage("subscription.plan.name.exists") + ": " + updatedPlan.getName());
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

        if (updatedPlan.getJobMatchLimit() != null) {
            existingPlan.setJobMatchLimit(updatedPlan.getJobMatchLimit());
        }

        if (updatedPlan.getTokenLimit() != null) {
            existingPlan.setTokenLimit(updatedPlan.getTokenLimit());
        }

        if (updatedPlan.getWatermark() != null) {
            existingPlan.setWatermark(updatedPlan.getWatermark());
        }



        if (updatedPlan.getMonthlyPrice() != null) {
            if (updatedPlan.getMonthlyPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("validation.price.negative");
            }
            existingPlan.setMonthlyPrice(updatedPlan.getMonthlyPrice());
        }

        if (updatedPlan.getAnnualPrice() != null) {
            if (updatedPlan.getAnnualPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("validation.price.negative");
            }
            existingPlan.setAnnualPrice(updatedPlan.getAnnualPrice());
        }

        // Billing restrictions
        if (updatedPlan.getAllowAnnualBilling() != null) {
            existingPlan.setAllowAnnualBilling(updatedPlan.getAllowAnnualBilling());
        }

        if (updatedPlan.getRenewalAllowed() != null) {
            existingPlan.setRenewalAllowed(updatedPlan.getRenewalAllowed());
        }

        if (updatedPlan.getMaxBillingCycles() != null) {
            existingPlan.setMaxBillingCycles(updatedPlan.getMaxBillingCycles());
        }

        // Display settings
        if (updatedPlan.getIsFeatured() != null) {
            existingPlan.setIsFeatured(updatedPlan.getIsFeatured());
        }

        if (updatedPlan.getSortOrder() != null) {
            existingPlan.setSortOrder(updatedPlan.getSortOrder());
        }

        // Linked group
        if (updatedPlan.getLinkedGroup() != null) {
            existingPlan.setLinkedGroup(updatedPlan.getLinkedGroup());
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
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

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
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

        // Prevent deactivating the free plan
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            throw new BadRequestException("subscription.plan.cannot.deactivate.free");
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
                .orElseThrow(() -> new ResourceNotFoundException("subscription.plan.notfound"));

        // Prevent deleting the free plan
        if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
            throw new BadRequestException("subscription.plan.cannot.delete.free");
        }

        // Check if there are any active subscriptions
        long activeSubscriptionCount = getActiveSubscriptionCountForPlan(planId);
        if (activeSubscriptionCount > 0) {
            throw new BadRequestException("subscription.plan.cannot.delete.active");
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
                throw new BadRequestException("subscription.plan.cannot.reduce.cvlimit");
            }
        }



        // Prevent removing features that were previously enabled
        if (Boolean.FALSE.equals(existingPlan.getWatermark()) && Boolean.TRUE.equals(updatedPlan.getWatermark())) {
            throw new BadRequestException("subscription.plan.cannot.add.watermark");
        }
        // Removed Dashboard validation
    }

}
