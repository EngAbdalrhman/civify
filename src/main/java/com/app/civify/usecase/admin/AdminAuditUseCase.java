package com.app.civify.usecase.admin;

import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

public interface AdminAuditUseCase {
    PagedResponse<AuditLogDTO> searchAuditLogs(AuditLogRequest request);
    PagedResponse<AuditLogDTO> getAuditHistory(String entityType, String entityId, InputDataModel inputData);
    PagedResponse<AuditLogDTO> getAuditHistoryByType(String entityType, InputDataModel inputData);
    PagedResponse<AuditLogDTO> getAllAuditLogs(InputDataModel inputData);
    AuditLogDTO getLatestAudit(String entityType, String entityId);
}
