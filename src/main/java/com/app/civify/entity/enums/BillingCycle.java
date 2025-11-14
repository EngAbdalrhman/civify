package com.app.civify.entity.enums;

/**
 * Billing cycle options for subscription plans
 */
public enum BillingCycle {
    /**
     * Monthly billing (1 month)
     */
    MONTHLY("Monthly", 1),
    
    /**
     * Quarterly billing (3 months)
     */
    QUARTERLY("Quarterly", 3),
    
    /**
     * Semi-annual billing (6 months)
     */
    SEMI_ANNUAL("Semi-Annual", 6),
    
    /**
     * Annual billing (12 months)
     */
    ANNUAL("Annual", 12),
    
    /**
     * Biennial billing (24 months)
     */
    BIENNIAL("Biennial", 24),
    
    /**
     * One-time payment (lifetime)
     */
    LIFETIME("Lifetime", -1);

    private final String displayName;
    private final int months;

    BillingCycle(String displayName, int months) {
        this.displayName = displayName;
        this.months = months;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMonths() {
        return months;
    }

    public boolean isLifetime() {
        return this == LIFETIME;
    }
}

