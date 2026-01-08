package com.app.civify.entity.enums;

/**
 * Types of subscription lifecycle events for audit logging
 */
public enum SubscriptionAuditType {
    /**
     * Subscription was renewed
     */
    RENEW,
    
    /**
     * Reminder email was sent
     */
    REMINDER_SENT,
    
    /**
     * User downgraded to free plan
     */
    DOWNGRADE,
    
    /**
     * Subscription expired
     */
    EXPIRE,
    
    /**
     * Zero-fee plan was auto-extended
     */
    AUTO_EXTEND,
    
    /**
     * Renewal invoice was created
     */
    INVOICE_CREATED,
    
    /**
     * Payment was received for renewal
     */
    PAYMENT_RECEIVED,
    
    /**
     * Subscription was activated
     */
    ACTIVATED,
    
    /**
     * Subscription was cancelled
     */
    CANCELLED
}
