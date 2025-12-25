package com.app.civify.repository;

import com.app.civify.entity.logs.AuditLog;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.util.persistence.CommonRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity
 * Extends CommonRepo to support generic filtering
 */
@Repository
public interface AuditLogRepository extends CommonRepo<AuditLog, Long> {

    /**
     * Find all audit logs for a specific entity
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    /**
     * Find all audit logs for a specific entity type
     */
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    /**
     * Find all audit logs performed by a specific user
     */
    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    /**
     * Find all audit logs for a specific action
     */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /**
     * Find all audit logs within a date range
     */
    Page<AuditLog> findByPerformedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find all audit logs for a specific entity and action
     */
    List<AuditLog> findByEntityTypeAndEntityIdAndAction(String entityType, String entityId, AuditAction action);

    /**
     * Find the most recent audit log for a specific entity
     */
    AuditLog findFirstByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, String entityId);
}

