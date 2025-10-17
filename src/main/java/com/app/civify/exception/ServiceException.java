package com.app.civify.exception;

/**
 * Exception thrown when a service operation fails
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new ServiceException with the specified message
     *
     * @param message The error message
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Create a new ServiceException with the specified message and cause
     *
     * @param message The error message
     * @param cause The cause of the exception
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new ServiceException for a specific service operation
     *
     * @param serviceName The name of the service
     * @param operation The operation that failed
     * @param reason The reason for the failure
     * @return A new ServiceException
     */
    public static ServiceException forOperation(String serviceName, String operation, String reason) {
        return new ServiceException(serviceName + " failed to " + operation + ": " + reason);
    }
}
