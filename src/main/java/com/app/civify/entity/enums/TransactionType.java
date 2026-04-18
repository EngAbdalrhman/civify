package com.app.civify.entity.enums;

/**
 * The type of a billing transaction in the unified billing architecture.
 */
public enum TransactionType {
    /**
     * Initial subscription purchase
     */
    SUBSCRIPTION,
    
    /**
     * Token top-up purchase
     */
    TOKEN_TOPUP,
    
    /**
     * Renewal of an existing subscription
     */
    RENEWAL,
    
    /**
     * Refund for a previous transaction
     */
    REFUND
}
