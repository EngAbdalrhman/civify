package com.app.civify.dto.audit;

import com.app.civify.entity.enums.AuditAction;
import com.app.civify.util.filter.FilterGroup;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for filtering audit logs Supports both simple filtering and
 * advanced filtering with FilterGroup
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class AuditLogRequest {

    // Simple filters
    private String entityType;
    private String entityId;
    private AuditAction action;
    private String performedBy;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Advanced filtering
    private FilterGroup filterGroup;

    // Pagination
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;

    // Sorting
    @Builder.Default
    private String sortBy = "performedAt";
    @Builder.Default
    private String sortDirection = "DESC";

}
