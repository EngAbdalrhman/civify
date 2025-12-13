package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.admin.AdminStatsDTO;
import com.app.civify.service.user.UserService;
import com.app.civify.service.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin controller for statistics
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin - Statistics", description = "Admin endpoints for system statistics")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminStatsController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Retrieves key statistics for the admin dashboard. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping
    public ResponseEntity<AdminStatsDTO> getStats() {
        log.info("Admin fetching dashboard statistics");

        // Gather stats from various services
        long totalUsers = userService.countTotalUsers();
        long subscribedUsers = subscriptionService.countActiveSubscriptions();
        long totalCVs = 0; // TODO: Implement CV service count
        long totalApiCalls = 0; // TODO: Implement API usage count
        long apiCallsThisMonth = 0;
        long activeSubscriptions = subscribedUsers;

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .subscribedUsers(subscribedUsers)
                .totalCVs(totalCVs)
                .totalApiCalls(totalApiCalls)
                .apiCallsThisMonth(apiCallsThisMonth)
                .activeSubscriptions(activeSubscriptions)
                .build();

        return ResponseEntity.ok(stats);
    }
}
