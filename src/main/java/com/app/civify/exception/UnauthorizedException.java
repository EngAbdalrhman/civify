package com.app.civify.exception;

/**
 * Exception thrown when a user is not authorized to perform an action
 */
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new UnauthorizedException with the specified message
     *
     * @param message The error message
     */
    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * Create a new UnauthorizedException with the specified message and cause
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
