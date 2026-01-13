package com.app.civify.service.subscription;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.FeatureType;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing subscriptions and plans
 * Implements Option A flow: single active subscription at a time
 */
public interface SubscriptionService {

    // Plan management
    List<SubscriptionPlan> getAllActivePlans();

    Optional<SubscriptionPlan> getPlanById(Long planId);

    Optional<SubscriptionPlan> getPlanByName(String name);

    // Subscription management
    UserSubscription createSubscription(Long userId, Long planId, String promoCode, boolean isAnnual);

    UserSubscription createSubscription(Long userId, Long planId, String promoCode, boolean isAnnual, String currency);

    /**
     * Create subscription using User entity directly (avoids extra DB fetch).
     */
    UserSubscription createSubscription(User user, Long planId, String promoCode, boolean isAnnual);

    UserSubscription createSubscription(User user, Long planId, String promoCode, boolean isAnnual, String currency);

    Optional<UserSubscription> getActiveSubscription(Long userId);

    List<UserSubscription> getUserSubscriptions(Long userId);

    long countActiveSubscriptions();

    // Payment and checkout
    PriceCalculationResponse calculateTotalPrice(Long userId, Long planId, boolean isAnnual, String promoCode);

    PriceCalculationResponse calculateTotalPrice(Long userId, Long planId, boolean isAnnual, String promoCode, String currency);

    PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway);

    PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, String currency);

    PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway, String currency, String paymentMethod);

    // Usage tracking
    boolean canUserUseFeature(Long userId, String feature); // CV, WATERMARK

    boolean canUserUseTokens(Long userId, int tokensNeeded);

    void incrementUsage(Long userId, String feature);

    void chargeTokens(Long userId, int tokensUsed);

    /**
     * Charge tokens with usage tracking.
     * @param userId User ID
     * @param tokensUsed Number of tokens to charge
     * @param usageType Type of usage (CV_PARSE, CV_TAILOR, etc.)
     * @param description Optional description for the usage
     */
    void chargeTokens(Long userId, int tokensUsed, TokenUsageType usageType, String description);

    /**
     * Check if user's current subscription has a specific feature enabled.
     * Used for plan-based access control (e.g., API access).
     * @param userId User ID
     * @param featureType Feature to check (e.g., API_ACCESS, WATERMARK_REMOVAL)
     * @return true if the user's active subscription includes this feature
     */
    boolean hasFeatureAccess(Long userId, FeatureType featureType);

    // Subscription lifecycle
    void activateSubscription(String paymentTransactionId);

    /**
     * Cancel user's active non-free subscription by setting autoRenew = false.
     * Subscription stays active until endDate, then user is downgraded to free plan.
     * @param userId The user ID
     * @throws ResourceNotFoundException if no active non-free subscription exists
     */
    void cancelActiveSubscription(Long userId);

    /**
     * Reactivate free plan for user (when paid subscription expires or is cancelled)
     */
    void reactivateFreePlan(Long userId);

    /**
     * Create a default free subscription for new users
     */
    void createDefaultFreeSubscription(Long userId);

    // ==================== ADMIN PLAN MANAGEMENT ====================

    /**
     * Create a new subscription plan (Admin only)
     */
    SubscriptionPlan createPlan(SubscriptionPlan plan);

    /**
     * Update an existing subscription plan (Admin only)
     * Validates that changes don't break existing active subscriptions
     */
    SubscriptionPlan updatePlan(Long planId, SubscriptionPlan updatedPlan);

    /**
     * Activate a subscription plan (Admin only)
     */
    SubscriptionPlan activatePlan(Long planId);

    /**
     * Deactivate a subscription plan (Admin only)
     * Does not affect existing active subscriptions
     */
    SubscriptionPlan deactivatePlan(Long planId);

    /**
     * Get all plans including inactive ones (Admin only)
     */
    List<SubscriptionPlan> getAllPlans();

    /**
     * Get count of active subscriptions for a specific plan
     */
    long getActiveSubscriptionCountForPlan(Long planId);

    /**
     * Soft delete a plan (Admin only)
     * Only allowed if no active subscriptions exist
     */
    void deletePlan(Long planId);

    /**
     * Filter plans with pagination and sorting (Admin only)
     */
    com.app.civify.util.filter.PagedResponse<SubscriptionPlan> filterPlans(com.app.civify.util.filter.InputDataModel inputData);


}
