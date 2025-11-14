package com.app.civify.dto.audit;

import com.app.civify.entity.enums.AuditAction;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for AuditLog responses
 * Excludes sensitive information and formats data for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogDTO {

    private Long id;
    private String entityType;
    private String entityId;
    private AuditAction action;
    private String performedBy;
    private LocalDateTime performedAt;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private Map<String, Object> changes;
    private String comment;
    private String ipAddress;
    private String userAgent;
}

