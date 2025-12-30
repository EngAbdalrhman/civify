package com.app.civify.controller.v1.logs;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.ApiKeyUsageLogDTO;
import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.logs.ApiUsageLogService.ApiUsageStatsDTO;
import com.app.civify.usecase.logs.ApiUsageLogUseCase;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for API Key Usage Logs
 * Delegates to ApiUsageLogUseCase for business logic
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/logs")
@RequiredArgsConstructor
@Tag(name = "API Usage Logs", description = "Endpoints for API usage logs")
public class ApiUsageLogController {

    private final ApiUsageLogUseCase apiUsageLogUseCase;

    /**
     * User endpoint: Get current user's API usage logs for current month
     */
    @Operation(
            summary = "Get my API usage logs",
            description = "Retrieves API usage logs for the authenticated user, auto-filtered by current month",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "API.ACCESS")
    @PostMapping("/my-logs/filter")
    public ResponseEntity<CommonResponse<PagedResponse<ApiKeyUsageLogDTO>>> getMyLogs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody InputDataModel inputData) {

        // Safely extract user from principal
        User user = userPrincipal != null ? userPrincipal.getUser() : null;
        if (user == null) {
            log.warn("API logs filter requested without valid user principal");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("User not authenticated"));
        }

        PagedResponse<ApiKeyUsageLogDTO> response = apiUsageLogUseCase.filterMyLogs(user, inputData);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    /**
     * User endpoint: Get current user's API usage stats
     */
    @Operation(
            summary = "Get my API usage stats",
            description = "Retrieves summary statistics for the authenticated user's API usage",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "CV.CREATE")
    @GetMapping("/my-stats")
    public ResponseEntity<CommonResponse<ApiUsageStatsDTO>> getMyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // Safely extract user from principal
        User user = userPrincipal != null ? userPrincipal.getUser() : null;
        if (user == null) {
            log.warn("API stats requested without valid user principal");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CommonResponse.error("User not authenticated"));
        }

        ApiUsageStatsDTO stats = apiUsageLogUseCase.getMyStats(user);
        return ResponseEntity.ok(CommonResponse.success(stats));
    }

    /**
     * Admin endpoint: Filter all API usage logs
     */
    @Operation(
            summary = "Filter API usage logs (Admin)",
            description = "Filter API usage logs with generic filter. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @PostMapping("/admin/filter")
    public ResponseEntity<CommonResponse<PagedResponse<ApiKeyUsageLogDTO>>> filterLogs(
            @RequestBody InputDataModel inputData) {
        
        PagedResponse<ApiKeyUsageLogDTO> response = apiUsageLogUseCase.filterAllLogs(inputData);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
