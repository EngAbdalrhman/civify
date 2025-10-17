package com.app.civify.exception;

/**
 * Exception thrown when attempting to create a resource that already exists
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new ResourceAlreadyExistsException with the specified message
     *
     * @param message The error message
     */
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * Create a new ResourceAlreadyExistsException with the specified message
     * and cause
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public ResourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new ResourceAlreadyExistsException for a resource with the
     * specified ID
     *
     * @param resourceName The name of the resource
     * @param fieldName The name of the field
     * @param fieldValue The value of the field
     * @return A new ResourceAlreadyExistsException
     */
    public static ResourceAlreadyExistsException forResource(String resourceName, String fieldName, Object fieldValue) {
        return new ResourceAlreadyExistsException(resourceName + " already exists with " + fieldName + ": " + fieldValue);
    }
}
