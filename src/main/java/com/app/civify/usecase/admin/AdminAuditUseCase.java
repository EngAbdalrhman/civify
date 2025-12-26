package com.app.civify.usecase.admin;

import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.util.filter.PagedResponse;

public interface AdminAuditUseCase {
    PagedResponse<AuditLogDTO> searchAuditLogs(AuditLogRequest request);
    PagedResponse<AuditLogDTO> getAuditHistory(String entityType, String entityId, int page, int size);
    PagedResponse<AuditLogDTO> getAuditHistoryByType(String entityType, int page, int size);
    AuditLogDTO getLatestAudit(String entityType, String entityId);
}
