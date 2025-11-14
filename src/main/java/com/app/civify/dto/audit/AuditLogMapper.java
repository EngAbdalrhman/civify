package com.app.civify.dto.audit;

import com.app.civify.entity.AuditLog;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between AuditLog entity and AuditLogDTO
 */
@Component
public class AuditLogMapper {

    /**
     * Convert AuditLog entity to AuditLogDTO
     */
    public AuditLogDTO toDTO(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }

        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .performedBy(auditLog.getPerformedBy())
                .performedAt(auditLog.getPerformedAt())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .changes(auditLog.getChanges())
                .comment(auditLog.getComment())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .build();
    }

    /**
     * Convert AuditLogDTO to AuditLog entity
     */
    public AuditLog toEntity(AuditLogDTO dto) {
        if (dto == null) {
            return null;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setId(dto.getId());
        auditLog.setEntityType(dto.getEntityType());
        auditLog.setEntityId(dto.getEntityId());
        auditLog.setAction(dto.getAction());
        auditLog.setPerformedBy(dto.getPerformedBy());
        auditLog.setPerformedAt(dto.getPerformedAt());
        auditLog.setOldValue(dto.getOldValue());
        auditLog.setNewValue(dto.getNewValue());
        auditLog.setChanges(dto.getChanges());
        auditLog.setComment(dto.getComment());
        auditLog.setIpAddress(dto.getIpAddress());
        auditLog.setUserAgent(dto.getUserAgent());

        return auditLog;
    }
}

