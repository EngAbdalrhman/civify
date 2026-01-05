package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.admin.AdminStatsDTO;
import com.app.civify.enums.StatsPeriod;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.admin.AdminStatsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin controller for statistics with time period filtering
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin - Statistics", description = "Admin endpoints for system statistics")
public class AdminStatsController {

    private final AdminStatsUseCase adminStatsUseCase;

    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Retrieves key statistics for the admin dashboard with optional time period filter. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "ADMIN_STATS.VIEW")
    @GetMapping
    public ResponseEntity<AdminStatsDTO> getStats(
            @Parameter(description = "Time period for statistics (MONTHLY, QUARTERLY, YEARLY, OVERALL)")
            @RequestParam(defaultValue = "MONTHLY") StatsPeriod period) {
        
        log.info("Admin fetching dashboard statistics for period: {}", period);

        AdminStatsDTO stats = adminStatsUseCase.getStats(period);

        return ResponseEntity.ok(stats);
    }
}
