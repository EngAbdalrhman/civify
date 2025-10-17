package com.app.civify.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new ResourceNotFoundException with the specified message
     *
     * @param message The error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Create a new ResourceNotFoundException with the specified message and
     * cause
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new ResourceNotFoundException for a resource with the specified
     * ID
     *
     * @param resourceName The name of the resource
     * @param fieldName The name of the field
     * @param fieldValue The value of the field
     * @return A new ResourceNotFoundException
     */
    public static ResourceNotFoundException forResource(String resourceName, String fieldName, Object fieldValue) {
        return new ResourceNotFoundException(resourceName + " not found with " + fieldName + ": " + fieldValue);
    }
}
