package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for API usage log management with advanced filtering
 *
 * Demonstrates usage of GenericFilterService with FilterGroup
 *
 * Example filter requests:
 *
 * 1. Filter by endpoint pattern and success status: POST /api/v1/logs/filter {
 * "filterGroup": { "filters": [ {"fieldName": "endpoint", "operation": "MATCH",
 * "value": "/api/v1/cv"}, {"fieldName": "success", "operation": "EQUAL",
 * "value": true} ], "operation": "AND" }, "page": 0, "size": 20, "sortBy":
 * "timestamp", "sortDirection": "DESC" }
 *
 * 2. Filter by date range: POST /api/v1/logs/filter { "filterGroup": {
 * "filters": [ {"fieldName": "timestamp", "operation": "DATE_BETWEEN", "value":
 * "2024-01-01", "value2": "2024-12-31"} ], "operation": "AND" } }
 *
 * 3. Complex nested filters (failed calls OR specific endpoint): POST
 * /api/v1/logs/filter { "filterGroup": { "groups": [ { "filters": [
 * {"fieldName": "success", "operation": "EQUAL", "value": false} ],
 * "operation": "OR" }, { "filters": [ {"fieldName": "endpoint", "operation":
 * "MATCH_EXACT", "value": "/api/v1/cv/generate"} ], "operation": "OR" } ],
 * "operation": "OR" } }
 */
@ApiV1Controller
@RequestMapping("/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LICENCED_USER')")
@Tag(name = "API Usage Logs", description = "API usage monitoring and analytics endpoints")
public class ApiUsageLogController {

    private final ApiUsageLogService apiUsageLogService;

    /**
     * Filter API usage logs with advanced criteria
     *
     * This endpoint demonstrates the full power of GenericFilterService
     */
    @Operation(
            summary = "Filter API usage logs",
            description = "Advanced filtering of API usage logs with complex criteria including date ranges, user filtering, and endpoint patterns.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs filtered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FilterResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/filter")
    public ResponseEntity<FilterResponse<ApiUsageLog>> filterLogs(@RequestBody FilterRequest request) {
        return ResponseEntity.ok(apiUsageLogService.filterLogs(request));
    }

    /**
     * Get logs for a specific user
     */
    @Operation(
            summary = "Get logs by user ID",
            description = "Retrieves all API usage logs for a specific user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiUsageLog.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ApiUsageLog>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(apiUsageLogService.getLogsByUserId(userId));
    }

    /**
     * Get logs for a user in a specific month
     */
    @Operation(
            summary = "Get logs by user and month",
            description = "Retrieves API usage logs for a specific user in a given month (format: YYYY-MM).",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiUsageLog.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
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
    @Operation(
            summary = "Get recent failed API calls",
            description = "Retrieves failed API calls from the last 24 hours for monitoring purposes.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Failed calls retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FilterResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
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
    @Operation(
            summary = "Get logs by endpoint pattern",
            description = "Retrieves API usage logs for endpoints matching a specific pattern.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FilterResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
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
    @Operation(
            summary = "Get logs by user email",
            description = "Retrieves API usage logs for a user by their email address using nested field filtering.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FilterResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
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
    @Operation(
            summary = "Count API calls by user and month",
            description = "Returns the total count of API calls made by a user in a specific month.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/user/{userId}/month/{month}/count")
    public ResponseEntity<Long> countApiCalls(@PathVariable Long userId, @PathVariable String month) {
        return ResponseEntity.ok(apiUsageLogService.countApiCallsByUserAndMonth(userId, month));
    }
}
