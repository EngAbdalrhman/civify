package com.app.civify.service.subscription;

import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.entity.SubscriptionPlan;
import com.app.civify.entity.UserSubscription;

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

    Optional<UserSubscription> getActiveSubscription(Long userId);

    List<UserSubscription> getUserSubscriptions(Long userId);

    long countActiveSubscriptions();

    // Payment and checkout
    PriceCalculationResponse calculateTotalPrice(Long planId, boolean isAnnual, String promoCode);

    PaymentInitiationResponse initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode, String gateway);

    // Usage tracking
    boolean canUserUseFeature(Long userId, String feature); // CV, API, WATERMARK, DASHBOARD

    void incrementUsage(Long userId, String feature);

    // Subscription lifecycle
    void activateSubscription(String paymentTransactionId);

    void cancelSubscription(Long subscriptionId);

    void renewSubscription(Long subscriptionId, boolean isAnnual);

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
}
