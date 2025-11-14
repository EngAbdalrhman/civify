package com.app.civify.entity.enums;

/**
 * Actions that can be audited in the system
 */
public enum AuditAction {
    /**
     * Entity was created
     */
    CREATE,
    
    /**
     * Entity was updated/modified
     */
    UPDATE,
    
    /**
     * Entity was deleted (soft or hard delete)
     */
    DELETE,
    
    /**
     * Entity was restored from soft delete
     */
    RESTORE,
    
    /**
     * User logged in
     */
    LOGIN,
    
    /**
     * User logged out
     */
    LOGOUT,
    
    /**
     * Password was changed
     */
    PASSWORD_CHANGE,
    
    /**
     * Email was verified
     */
    EMAIL_VERIFIED,
    
    /**
     * 2FA was enabled
     */
    TWO_FACTOR_ENABLED,
    
    /**
     * 2FA was disabled
     */
    TWO_FACTOR_DISABLED,
    
    /**
     * Role was changed
     */
    ROLE_CHANGE,
    
    /**
     * Subscription was activated
     */
    SUBSCRIPTION_ACTIVATED,
    
    /**
     * Subscription was cancelled
     */
    SUBSCRIPTION_CANCELLED,
    
    /**
     * Payment was completed
     */
    PAYMENT_COMPLETED,
    
    /**
     * Payment failed
     */
    PAYMENT_FAILED,
    
    /**
     * Other custom actions
     */
    OTHER
}

