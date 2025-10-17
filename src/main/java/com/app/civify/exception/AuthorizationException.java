package com.app.civify.exception;

/**
 * Custom exception for authorization-related operations
 *
 * @author Code Review Enhancement
 * @since 1.0.0
 */
public class AuthorizationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public AuthorizationException(String message) {
        super(message);
        this.errorCode = "AUTHORIZATION_ERROR";
    }

    public AuthorizationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AUTHORIZATION_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
