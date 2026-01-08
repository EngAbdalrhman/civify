package com.app.civify.controller.v1.logs;

import com.app.civify.dto.token.TokenUsageDto;
import com.app.civify.dto.token.TokenUsageSummaryDto;
import com.app.civify.entity.User;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.logs.AuditLogUseCase;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Token Usage", description = "Token usage tracking endpoints")
public class TokenUsageController {

    private final AuditLogUseCase auditLogUseCase;

    @Operation(summary = "Get my token usage history")
    @GetMapping("/users/me/token-usage")
    public ResponseEntity<Page<TokenUsageDto>> getMyTokenUsage(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TokenUsageDto> usage = auditLogUseCase.getMyTokenUsageHistory(user, page, size);
        return ResponseEntity.ok(usage);
    }

    @Operation(summary = "Get my monthly token usage summary")
    @GetMapping("/users/me/token-usage/summary")
    public ResponseEntity<TokenUsageSummaryDto> getMyTokenUsageSummary(
            @AuthenticationPrincipal User user) {
        TokenUsageSummaryDto summary = auditLogUseCase.getMyTokenUsageSummary(user);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get all token usage logs (Admin)")
    @PostMapping("/admin/token-usage/filter")
    @CivifyAuthorization(permission = "ADMIN_STATS.VIEW")
    public ResponseEntity<PagedResponse<TokenUsageDto>> filterTokenUsage(
            @RequestBody InputDataModel inputData) {
        PagedResponse<TokenUsageDto> usage = auditLogUseCase.filterTokenUsageLogs(inputData);
        return ResponseEntity.ok(usage);
    }

    @Operation(summary = "Get specific user's token usage (Admin)")
    @GetMapping("/admin/token-usage/user/{userId}")
    @CivifyAuthorization(permission = "ADMIN_STATS.VIEW")
    public ResponseEntity<Page<TokenUsageDto>> getUserTokenUsage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TokenUsageDto> usage = auditLogUseCase.getUserTokenUsageHistory(userId, page, size);
        return ResponseEntity.ok(usage);
    }
}
