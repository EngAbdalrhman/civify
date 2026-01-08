package com.app.civify.entity.enums;

/**
 * Source from where tokens were deducted
 */
public enum TokenSource {
    /**
     * Tokens from monthly subscription allowance
     */
    SUBSCRIPTION,
    
    /**
     * Tokens from purchased wallet/extra tokens
     */
    WALLET,
    
    /**
     * Combination of subscription and wallet
     */
    MIXED
}
