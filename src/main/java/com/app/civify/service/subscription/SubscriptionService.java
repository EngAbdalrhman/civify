package com.app.civify.service.subscription;

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

    // Payment and checkout
    PriceCalculationResponse calculateTotalPrice(Long planId, boolean isAnnual, String promoCode);

    String initiatePayment(Long userId, Long planId, boolean isAnnual, String promoCode);

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
}
