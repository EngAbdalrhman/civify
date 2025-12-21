package com.app.civify.exception;

/**
 * Centralized error codes for consistent API error responses.
 * Frontend can use these codes to:
 * 1. Detect specific error types reliably (instead of string matching)
 * 2. Map codes to localized messages
 */
public enum ErrorCode {
    
    // ============== Authentication & Authorization ==============
    AUTH_EMAIL_NOT_VERIFIED("AUTH_001", "auth.emailNotVerified"),
    AUTH_INVALID_CREDENTIALS("AUTH_002", "auth.invalidCredentials"),
    AUTH_TOKEN_EXPIRED("AUTH_003", "auth.tokenExpired"),
    AUTH_TOKEN_INVALID("AUTH_004", "auth.tokenInvalid"),
    AUTH_UNAUTHORIZED("AUTH_005", "auth.unauthorized"),
    AUTH_ACCOUNT_DISABLED("AUTH_006", "auth.accountDisabled"),
    AUTH_2FA_REQUIRED("AUTH_007", "auth.twoFactorRequired"),
    AUTH_2FA_INVALID("AUTH_008", "auth.twoFactorInvalid"),
    
    // ============== Validation ==============
    VALIDATION_FAILED("VAL_001", "validation.failed"),
    VALIDATION_EMAIL_EXISTS("VAL_002", "validation.emailExists"),
    VALIDATION_USERNAME_EXISTS("VAL_003", "validation.usernameExists"),
    VALIDATION_INVALID_INPUT("VAL_004", "validation.invalidInput"),
    
    // ============== Resource Errors ==============
    RESOURCE_NOT_FOUND("RES_001", "resource.notFound"),
    RESOURCE_ALREADY_EXISTS("RES_002", "resource.alreadyExists"),
    RESOURCE_ACCESS_DENIED("RES_003", "resource.accessDenied"),
    
    // ============== Payment ==============
    PAYMENT_FAILED("PAY_001", "payment.failed"),
    PAYMENT_INVALID_METHOD("PAY_002", "payment.invalidMethod"),
    SUBSCRIPTION_EXPIRED("PAY_003", "subscription.expired"),
    
    // ============== General Errors ==============
    INTERNAL_ERROR("GEN_001", "error.internal"),
    BAD_REQUEST("GEN_002", "error.badRequest"),
    SERVICE_UNAVAILABLE("GEN_003", "error.serviceUnavailable");
    
    private final String code;
    private final String messageKey;
    
    ErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessageKey() {
        return messageKey;
    }
    
    /**
     * Find ErrorCode by its code string
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
