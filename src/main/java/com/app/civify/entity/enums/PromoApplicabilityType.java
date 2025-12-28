package com.app.civify.entity.enums;

/**
 * Defines what a promo code can be applied to.
 */
public enum PromoApplicabilityType {
    /**
     * Promo can be used for subscription plans only
     */
    PLAN,
    
    /**
     * Promo can be used for extra token purchases only
     */
    TOKEN,
    
    /**
     * Promo can be used for both plans and tokens
     */
    ALL
}
