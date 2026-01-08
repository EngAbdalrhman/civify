package com.app.civify.controller.v1.admin;

import com.app.civify.dto.audit.BillingAuditDto;
import com.app.civify.dto.audit.SubscriptionAuditDto;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.logs.AuditLogUseCase;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Admin Audit Logs", description = "Admin endpoints for viewing audit logs")
public class AdminAuditLogsController {

    private final AuditLogUseCase auditLogUseCase;

    @Operation(summary = "Get subscription audit logs with filtering")
    @PostMapping("/subscription/filter")
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    public ResponseEntity<PagedResponse<SubscriptionAuditDto>> filterSubscriptionAuditLogs(
            @RequestBody InputDataModel inputData) {
        PagedResponse<SubscriptionAuditDto> logs = auditLogUseCase.filterSubscriptionAuditLogs(inputData);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get billing audit logs with filtering")
    @PostMapping("/billing/filter")
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    public ResponseEntity<PagedResponse<BillingAuditDto>> filterBillingAuditLogs(
            @RequestBody InputDataModel inputData) {
        PagedResponse<BillingAuditDto> logs = auditLogUseCase.filterBillingAuditLogs(inputData);
        return ResponseEntity.ok(logs);
    }
}
