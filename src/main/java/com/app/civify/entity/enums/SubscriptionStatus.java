package com.app.civify.entity.enums;

/**
 * Status of a user subscription
 */
public enum SubscriptionStatus {
    /**
     * Subscription is active and can be used
     */
    ACTIVE,
    
    /**
     * Subscription has expired naturally (end date passed)
     */
    EXPIRED,
    
    /**
     * Subscription was cancelled by user or system
     */
    CANCELLED,
    
    /**
     * Subscription is created but payment not yet completed
     */
    PENDING
}
