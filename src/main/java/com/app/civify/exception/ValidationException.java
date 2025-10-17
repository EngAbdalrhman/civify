package com.app.civify.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> errors;

    /**
     * Create a new ValidationException with the specified message
     *
     * @param message The error message
     */
    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    /**
     * Create a new ValidationException with the specified message and errors
     *
     * @param message The error message
     * @param errors Map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    /**
     * Create a new ValidationException with the specified message, errors, and
     * cause
     *
     * @param message The error message
     * @param errors Map of field names to error messages
     * @param cause The cause of the exception
     */
    public ValidationException(String message, Map<String, String> errors, Throwable cause) {
        super(message, cause);
        this.errors = errors;
    }

    /**
     * Get the validation errors
     *
     * @return Map of field names to error messages
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    /**
     * Add a validation error
     *
     * @param field The field name
     * @param message The error message
     * @return This ValidationException for chaining
     */
    public ValidationException addError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }
}
