package com.app.civify.exception;

/**
 * Custom exception for entity not found operations
 *
 * @author Code Review Enhancement
 * @since 1.0.0
 */
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String entityType;
    private final Object entityId;

    public EntityNotFoundException(String entityType, Object entityId) {
        super(String.format("%s with id %s not found", entityType, entityId));
        this.errorCode = "ENTITY_NOT_FOUND";
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.errorCode = "ENTITY_NOT_FOUND";
        this.entityType = "Unknown";
        this.entityId = null;
    }

    public EntityNotFoundException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.entityType = "Unknown";
        this.entityId = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getEntityId() {
        return entityId;
    }
}
