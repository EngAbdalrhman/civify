package com.app.civify.dto.audit;

import com.app.civify.entity.enums.AuditAction;
import com.app.civify.util.filter.FilterGroup;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer page = 0;
    private Integer size = 20;

    // Sorting
    private String sortBy = "performedAt";
    private String sortDirection = "DESC";

    // Manual getters (Lombok compatibility)
    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public FilterGroup getFilterGroup() {
        return filterGroup;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    // Manual setters
    public void setPage(Integer page) {
        this.page = page;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
