package com.app.civify.exception;

/**
 * Exception thrown when a request is invalid
 */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new BadRequestException with the specified message
     *
     * @param message The error message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Create a new BadRequestException with the specified message and cause
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
