package com.app.civify.usecase.admin;

import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.service.audit.AuditService;
import com.app.civify.util.filter.InputDataModel;
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
    public PagedResponse<AuditLogDTO> getAuditHistory(String entityType, String entityId, InputDataModel inputData) {
        log.debug("UseCase: Retrieving audit logs for {}/{}", entityType, entityId);
        
        // Add entity type and ID filters to inputData
        inputData.addFilter("entityType", com.app.civify.util.filter.FilterOperation.EQUAL, entityType);
        inputData.addFilter("entityId", com.app.civify.util.filter.FilterOperation.EQUAL, entityId);
        
        AuditLogRequest request = new AuditLogRequest();
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setPage(inputData.getPage());
        request.setSize(inputData.getSize());
        
        return auditService.getAuditHistory(request);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAuditHistoryByType(String entityType, InputDataModel inputData) {
        log.debug("UseCase: Retrieving audit logs for entity type: {}", entityType);
        
        inputData.addFilter("entityType", com.app.civify.util.filter.FilterOperation.EQUAL, entityType);
        
        AuditLogRequest request = new AuditLogRequest();
        request.setEntityType(entityType);
        request.setPage(inputData.getPage());
        request.setSize(inputData.getSize());
        
        return auditService.getAuditHistory(request);
    }

    @Override
    public PagedResponse<AuditLogDTO> getAllAuditLogs(InputDataModel inputData) {
        log.debug("UseCase: Retrieving all audit logs with pagination");
        
        AuditLogRequest request = new AuditLogRequest();
        request.setPage(inputData.getPage());
        request.setSize(inputData.getSize());
        
        // Use sorts from inputData if available
        if (!inputData.getSorts().isEmpty()) {
            request.setSortBy(inputData.getSorts().get(0).getFieldName());
            request.setSortDirection(inputData.getSorts().get(0).getDirection().name());
        }
        
        return auditService.getAuditHistory(request);
    }

    @Override
    public AuditLogDTO getLatestAudit(String entityType, String entityId) { // Fixed generic/return type mismatch if any
        log.debug("UseCase: Retrieving latest audit log for {}/{}", entityType, entityId);
        return auditService.getLatestAudit(entityType, entityId);
    }
}
