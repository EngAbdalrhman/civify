package com.app.civify.controller.v1.logs;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.audit.AuditLogDTO;
import com.app.civify.dto.audit.AuditLogRequest;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.admin.AdminAuditUseCase;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for audit log management Provides endpoints for viewing and
 * filtering audit logs
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit Logs", description = "Admin endpoints for audit log management")
public class AdminAuditController {

    private final AdminAuditUseCase adminAuditUseCase;

    @Operation(
            summary = "Get audit logs with filtering",
            description = "Retrieve audit logs with advanced filtering, pagination, and sorting. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @PostMapping("/search")
    public ResponseEntity<PagedResponse<AuditLogDTO>> searchAuditLogs(
            @RequestBody AuditLogRequest request) {
        log.info("Admin searching audit logs with filters: entityType={}, entityId={}, action={}",
                request.getEntityType(), request.getEntityId(), request.getAction());

        PagedResponse<AuditLogDTO> response = adminAuditUseCase.searchAuditLogs(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get audit logs for specific entity",
            description = "Retrieve all audit logs for a specific entity by type and ID. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @PostMapping("/{entityType}/{entityId}")
    public ResponseEntity<PagedResponse<AuditLogDTO>> getAuditLogsForEntity(
            @Parameter(description = "Entity type (e.g., User, SubscriptionPlan)")
            @PathVariable String entityType,
            @Parameter(description = "Entity ID")
            @PathVariable String entityId,
            @RequestBody InputDataModel inputData) {

        log.info("Admin retrieving audit logs for {}/{}", entityType, entityId);

        PagedResponse<AuditLogDTO> response = adminAuditUseCase.getAuditHistory(entityType, entityId, inputData);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get audit logs by entity type",
            description = "Retrieve all audit logs for a specific entity type. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @PostMapping("/{entityType}")
    public ResponseEntity<PagedResponse<AuditLogDTO>> getAuditLogsByType(
            @Parameter(description = "Entity type (e.g., User, SubscriptionPlan)")
            @PathVariable String entityType,
            @RequestBody InputDataModel inputData) {

        log.info("Admin retrieving audit logs for entity type: {}", entityType);

        PagedResponse<AuditLogDTO> response = adminAuditUseCase.getAuditHistoryByType(entityType, inputData);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get latest audit log for entity",
            description = "Retrieve the most recent audit log for a specific entity. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @GetMapping("/{entityType}/{entityId}/latest")
    public ResponseEntity<AuditLogDTO> getLatestAuditLog(
            @Parameter(description = "Entity type (e.g., User, SubscriptionPlan)")
            @PathVariable String entityType,
            @Parameter(description = "Entity ID")
            @PathVariable String entityId) {

        log.info("Admin retrieving latest audit log for {}/{}", entityType, entityId);

        AuditLogDTO auditLog = adminAuditUseCase.getLatestAudit(entityType, entityId);
        if (auditLog == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(auditLog);
    }

    @Operation(
            summary = "Get all audit logs",
            description = "Retrieve all audit logs with pagination. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @PostMapping("/all")
    public ResponseEntity<PagedResponse<AuditLogDTO>> getAllAuditLogs(@RequestBody InputDataModel inputData) {

        log.info("Admin retrieving all audit logs with pagination: page={}, size={}", inputData.getPage(), inputData.getSize());

        PagedResponse<AuditLogDTO> response = adminAuditUseCase.getAllAuditLogs(inputData);
        return ResponseEntity.ok(response);
    }
}
