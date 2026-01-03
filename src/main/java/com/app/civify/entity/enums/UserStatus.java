package com.app.civify.entity.enums;

/**
 * User account status enum
 * Used to track the state of user accounts
 */
public enum UserStatus {
    /**
     * Active user - normal state, can use the platform
     */
    ACTIVE,
    
    /**
     * Disabled user - manually disabled by admin (banned)
     */
    DISABLED
}
