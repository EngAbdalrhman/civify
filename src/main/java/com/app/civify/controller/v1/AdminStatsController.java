package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.admin.AdminStatsDTO;
import com.app.civify.entity.enums.CvOperationAction;
import com.app.civify.repository.ApiKeyUsageLogRepository;
import com.app.civify.repository.CvOperationLogRepository;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.user.UserService;
import com.app.civify.service.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * Admin controller for statistics with time period filtering
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin - Statistics", description = "Admin endpoints for system statistics")
public class AdminStatsController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final CvOperationLogRepository cvOperationLogRepository;
    private final ApiKeyUsageLogRepository apiKeyUsageLogRepository;

    /**
     * Time period options for statistics
     */
    public enum StatsPeriod {
        MONTHLY,    // Current month
        QUARTERLY,  // Current quarter
        YEARLY,     // Current year
        OVERALL     // All time
    }

    @Operation(
            summary = "Get admin dashboard statistics",
            description = "Retrieves key statistics for the admin dashboard with optional time period filter. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "SYSTEM.LOGS.VIEW")
    @GetMapping
    public ResponseEntity<AdminStatsDTO> getStats(
            @Parameter(description = "Time period for statistics (MONTHLY, QUARTERLY, YEARLY, OVERALL)")
            @RequestParam(defaultValue = "MONTHLY") StatsPeriod period) {
        
        log.info("Admin fetching dashboard statistics for period: {}", period);

        // Calculate date range based on period
        LocalDateTime startDate = calculateStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        // Gather stats from various services
        long totalUsers = userService.countTotalUsers();
        long subscribedUsers = subscriptionService.countActiveSubscriptions();
        
        // CV counts based on period
        long totalCVs;
        long totalApiCalls;
        long apiCallsThisMonth;
        
        if (period == StatsPeriod.OVERALL) {
            // All time stats
            totalCVs = cvOperationLogRepository.countByAction(CvOperationAction.UPLOAD);
            totalApiCalls = cvOperationLogRepository.count();
            
            // This month for the "apiCallsThisMonth" field
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            apiCallsThisMonth = cvOperationLogRepository.findByDateRange(
                startOfMonth, endDate, Pageable.unpaged()
            ).getTotalElements();
        } else {
            // Period-based stats
            long periodCVs = countOperationsInPeriod(CvOperationAction.UPLOAD, startDate, endDate);
            long periodApiCalls = apiKeyUsageLogRepository.countByDateRange(startDate, endDate);
            
            totalCVs = periodCVs;
            totalApiCalls = periodApiCalls;
            apiCallsThisMonth = periodApiCalls; // Same as period stats
        }

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .subscribedUsers(subscribedUsers)
                .totalCVs(totalCVs)
                .totalApiCalls(totalApiCalls)
                .apiCallsThisMonth(apiCallsThisMonth)
                .activeSubscriptions(subscribedUsers)
                .period(period.name())
                .periodStart(startDate != null ? startDate.toString() : null)
                .periodEnd(endDate.toString())
                .build();

        return ResponseEntity.ok(stats);
    }
    
    /**
     * Calculate start date based on the selected period
     */
    private LocalDateTime calculateStartDate(StatsPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (period) {
            case MONTHLY -> now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case QUARTERLY -> {
                int currentMonth = now.getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                yield now.withMonth(quarterStartMonth).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
            case YEARLY -> now.withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case OVERALL -> null; // No start date for overall
        };
    }
    
    /**
     * Count operations within a date range
     */
    private long countOperationsInPeriod(CvOperationAction action, LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return cvOperationLogRepository.countByAction(action);
        }
        // Use the date range query and filter by action
        return cvOperationLogRepository.findByDateRange(start, end, Pageable.unpaged())
                .stream()
                .filter(log -> log.getAction() == action)
                .count();
    }
}
