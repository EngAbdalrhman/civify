package com.app.civify.controller.v1;

import com.app.civify.dto.filter.FilterRequest;
import com.app.civify.dto.filter.FilterResponse;
import com.app.civify.entity.ApiUsageLog;
import com.app.civify.service.ApiUsageLogService;
import com.app.civify.util.filter.FilterGroup;
import com.app.civify.util.filter.FilterOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for API usage log management with advanced filtering
 *
 * Demonstrates usage of GenericFilterService with FilterGroup
 *
 * Example filter requests:
 *
 * 1. Filter by endpoint pattern and success status:
 * POST /api/v1/logs/filter
 * {
 *   "filterGroup": {
 *     "filters": [
 *       {"fieldName": "endpoint", "operation": "MATCH", "value": "/api/v1/cv"},
 *       {"fieldName": "success", "operation": "EQUAL", "value": true}
 *     ],
 *     "operation": "AND"
 *   },
 *   "page": 0,
 *   "size": 20,
 *   "sortBy": "timestamp",
 *   "sortDirection": "DESC"
 * }
 *
 * 2. Filter by date range:
 * POST /api/v1/logs/filter
 * {
 *   "filterGroup": {
 *     "filters": [
 *       {"fieldName": "timestamp", "operation": "DATE_BETWEEN", "value": "2024-01-01", "value2": "2024-12-31"}
 *     ],
 *     "operation": "AND"
 *   }
 * }
 *
 * 3. Complex nested filters (failed calls OR specific endpoint):
 * POST /api/v1/logs/filter
 * {
 *   "filterGroup": {
 *     "groups": [
 *       {
 *         "filters": [
 *           {"fieldName": "success", "operation": "EQUAL", "value": false}
 *         ],
 *         "operation": "OR"
 *       },
 *       {
 *         "filters": [
 *           {"fieldName": "endpoint", "operation": "MATCH_EXACT", "value": "/api/v1/cv/generate"}
 *         ],
 *         "operation": "OR"
 *       }
 *     ],
 *     "operation": "OR"
 *   }
 * }
 */
@RestController
@ApiV1Controller
@RequestMapping("/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiUsageLogController {

    private final ApiUsageLogService apiUsageLogService;

    /**
     * Filter API usage logs with advanced criteria
     *
     * This endpoint demonstrates the full power of GenericFilterService
     */
    @PostMapping("/filter")
    public ResponseEntity<FilterResponse<ApiUsageLog>> filterLogs(@RequestBody FilterRequest request) {
        return ResponseEntity.ok(apiUsageLogService.filterLogs(request));
    }

    /**
     * Get logs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ApiUsageLog>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(apiUsageLogService.getLogsByUserId(userId));
    }

    /**
     * Get logs for a user in a specific month
     */
    @GetMapping("/user/{userId}/month/{month}")
    public ResponseEntity<List<ApiUsageLog>> getLogsByUserAndMonth(
            @PathVariable Long userId,
            @PathVariable String month) {
        return ResponseEntity.ok(apiUsageLogService.getLogsByUserAndMonth(userId, month));
    }

    /**
     * Quick filter: Get failed API calls in the last 24 hours
     *
     * Demonstrates programmatic usage of FilterGroup
     */
    @GetMapping("/failed/recent")
    public ResponseEntity<FilterResponse<ApiUsageLog>> getRecentFailedCalls() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        FilterGroup group = FilterGroup.and()
                .addFilter("success", FilterOperation.EQUAL, false)
                .addFilter("timestamp", FilterOperation.DATE_GREATER_THAN, yesterday);

        FilterRequest request = FilterRequest.builder()
                .filterGroup(group)
                .page(0)
                .size(50)
                .sortBy("timestamp")
                .sortDirection(Sort.Direction.DESC)
                .build();

        return ResponseEntity.ok(apiUsageLogService.filterLogs(request));
    }

    /**
     * Quick filter: Get logs for specific endpoint pattern
     *
     * Demonstrates URL parameter to filter conversion
     */
    @GetMapping("/endpoint")
    public ResponseEntity<FilterResponse<ApiUsageLog>> getLogsByEndpoint(
            @RequestParam String pattern,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        FilterGroup group = FilterGroup.and()
                .addFilter("endpoint", FilterOperation.MATCH, pattern);

        FilterRequest request = FilterRequest.builder()
                .filterGroup(group)
                .page(page)
                .size(size)
                .sortBy("timestamp")
                .sortDirection(Sort.Direction.DESC)
                .build();

        return ResponseEntity.ok(apiUsageLogService.filterLogs(request));
    }

    /**
     * Quick filter: Get logs by user with nested field access
     *
     * Demonstrates nested field filtering (user.email)
     */
    @GetMapping("/user-email")
    public ResponseEntity<FilterResponse<ApiUsageLog>> getLogsByUserEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        FilterGroup group = FilterGroup.and()
                .addFilter("user.email", FilterOperation.MATCH, email);

        FilterRequest request = FilterRequest.builder()
                .filterGroup(group)
                .page(page)
                .size(size)
                .sortBy("timestamp")
                .sortDirection(Sort.Direction.DESC)
                .build();

        return ResponseEntity.ok(apiUsageLogService.filterLogs(request));
    }

    /**
     * Count API calls for a user in a specific month
     */
    @GetMapping("/user/{userId}/month/{month}/count")
    public ResponseEntity<Long> countApiCalls(@PathVariable Long userId, @PathVariable String month) {
        return ResponseEntity.ok(apiUsageLogService.countApiCallsByUserAndMonth(userId, month));
    }
}
