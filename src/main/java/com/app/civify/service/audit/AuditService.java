package com.app.civify.service.audit;

import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.util.filter.PagedResponse;

import java.util.Map;

/**
 * Service interface for audit logging
 * Provides methods for logging entity changes and retrieving audit history
 */
public interface AuditService {

    /**
     * Log an audit entry for an entity change
     *
     * @param entityType Type of entity (e.g., "User", "SubscriptionPlan")
     * @param entityId ID of the entity
     * @param action Action performed
     * @param oldValue Old value of the entity (before change)
     * @param newValue New value of the entity (after change)
     * @param comment Optional comment
     */
    void logAudit(String entityType, String entityId, AuditAction action,
                  Map<String, Object> oldValue, Map<String, Object> newValue, String comment);

    /**
     * Log an audit entry with automatic change detection
     *
     * @param entityType Type of entity
     * @param entityId ID of the entity
     * @param action Action performed
     * @param oldValue Old value of the entity
     * @param newValue New value of the entity
     */
    void logAudit(String entityType, String entityId, AuditAction action,
                  Map<String, Object> oldValue, Map<String, Object> newValue);

    /**
     * Log a simple audit entry without old/new values
     *
     * @param entityType Type of entity
     * @param entityId ID of the entity
     * @param action Action performed
     * @param comment Optional comment
     */
    void logAudit(String entityType, String entityId, AuditAction action, String comment);

    /**
     * Get audit history for a specific entity
     *
     * @param entityType Type of entity
     * @param entityId ID of the entity
     * @param page Page number
     * @param size Page size
     * @return Paged audit logs
     */
    PagedResponse<AuditLogDTO> getAuditHistory(String entityType, String entityId, int page, int size);

    /**
     * Get audit history for a specific entity type
     *
     * @param entityType Type of entity
     * @param page Page number
     * @param size Page size
     * @return Paged audit logs
     */
    PagedResponse<AuditLogDTO> getAuditHistoryByType(String entityType, int page, int size);

    /**
     * Get audit history with advanced filtering
     *
     * @param request Filter request with pagination and sorting
     * @return Paged audit logs
     */
    PagedResponse<AuditLogDTO> getAuditHistory(AuditLogRequest request);

    /**
     * Get the most recent audit log for a specific entity
     *
     * @param entityType Type of entity
     * @param entityId ID of the entity
     * @return Most recent audit log or null
     */
    AuditLogDTO getLatestAudit(String entityType, String entityId);

    /**
     * Delete old audit logs (for cleanup/archival)
     *
     * @param daysToKeep Number of days to keep audit logs
     * @return Number of deleted records
     */
    int deleteOldAuditLogs(int daysToKeep);
}

