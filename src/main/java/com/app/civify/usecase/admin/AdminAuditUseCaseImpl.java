package com.app.civify.usecase.admin;

import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.service.audit.AuditService;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditUseCaseImpl extends CommonService implements AdminAuditUseCase {

    private final AuditService auditService;

    @Override
    public PagedResponse<AuditLogDTO> searchAuditLogs(AuditLogRequest request) {
        log.debug("UseCase: Searching audit logs with filters: entityType={}, entityId={}, action={}",
                request.getEntityType(), request.getEntityId(), request.getAction());
        return auditService.getAuditHistory(request);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAuditHistory(String entityType, String entityId, int page, int size) {
        log.debug("UseCase: Retrieving audit logs for {}/{}", entityType, entityId);
        return auditService.getAuditHistory(entityType, entityId, page, size);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAuditHistoryByType(String entityType, int page, int size) {
        log.debug("UseCase: Retrieving audit logs for entity type: {}", entityType);
        return auditService.getAuditHistoryByType(entityType, page, size);
    }

    @Override
    public AuditLogDTO getLatestAudit(String entityType, String entityId) { // Fixed generic/return type mismatch if any
        log.debug("UseCase: Retrieving latest audit log for {}/{}", entityType, entityId);
        return auditService.getLatestAudit(entityType, entityId);
    }
}
