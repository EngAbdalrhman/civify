package com.app.civify.controller.v1;

import com.app.civify.entity.SubscriptionPlan;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing subscription plans
 * All endpoints require ADMIN role
 */
@RestController
@ApiV1Controller
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin - Plan Management", description = "Admin endpoints for managing subscription plans")
public class AdminPlanController {

    private final SubscriptionService subscriptionService;

    @Operation(
            summary = "Get all subscription plans (including inactive)",
            description = "Retrieves all subscription plans including inactive ones. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plans retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden - Admin access required",
                content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        log.info("Admin fetching all subscription plans");
        List<SubscriptionPlan> plans = subscriptionService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @Operation(
            summary = "Get plan with subscription statistics",
            description = "Retrieves a plan with active subscription count. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan retrieved successfully",
                content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plan not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/{planId}")
    public ResponseEntity<Map<String, Object>> getPlanWithStats(@PathVariable Long planId) {
        log.info("Admin fetching plan {} with statistics", planId);
        
        return subscriptionService.getPlanById(planId)
                .map(plan -> {
                    long activeSubscriptionCount = subscriptionService.getActiveSubscriptionCountForPlan(planId);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("plan", plan);
                    response.put("activeSubscriptionCount", activeSubscriptionCount);
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create a new subscription plan",
            description = "Creates a new subscription plan. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Plan created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid plan data",
                content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlan>> createPlan(@RequestBody SubscriptionPlan plan) {
        log.info("Admin creating new subscription plan: {}", plan.getName());
        
        try {
            SubscriptionPlan createdPlan = subscriptionService.createPlan(plan);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Plan created successfully", createdPlan));
        } catch (Exception e) {
            log.error("Error creating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Update an existing subscription plan",
            description = "Updates a subscription plan. Validates that changes don't break existing subscriptions. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid update - would break existing subscriptions",
                content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plan not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> updatePlan(
            @PathVariable Long planId,
            @RequestBody SubscriptionPlan updatedPlan) {
        log.info("Admin updating subscription plan: {}", planId);
        
        try {
            SubscriptionPlan updated = subscriptionService.updatePlan(planId, updatedPlan);
            return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", updated));
        } catch (Exception e) {
            log.error("Error updating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Activate a subscription plan",
            description = "Activates a subscription plan, making it available for new subscriptions. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan activated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plan not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @PatchMapping("/{planId}/activate")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> activatePlan(@PathVariable Long planId) {
        log.info("Admin activating subscription plan: {}", planId);
        
        try {
            SubscriptionPlan activated = subscriptionService.activatePlan(planId);
            return ResponseEntity.ok(ApiResponse.success("Plan activated successfully", activated));
        } catch (Exception e) {
            log.error("Error activating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Deactivate a subscription plan",
            description = "Deactivates a subscription plan. Existing subscriptions remain active. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan deactivated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Cannot deactivate free plan",
                content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plan not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @PatchMapping("/{planId}/deactivate")
    public ResponseEntity<ApiResponse<SubscriptionPlan>> deactivatePlan(@PathVariable Long planId) {
        log.info("Admin deactivating subscription plan: {}", planId);
        
        try {
            SubscriptionPlan deactivated = subscriptionService.deactivatePlan(planId);
            return ResponseEntity.ok(ApiResponse.success("Plan deactivated successfully", deactivated));
        } catch (Exception e) {
            log.error("Error deactivating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Delete a subscription plan",
            description = "Soft deletes a subscription plan. Only allowed if no active subscriptions exist. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan deleted successfully",
                content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Cannot delete plan with active subscriptions or free plan",
                content = @Content(mediaType = "application/json")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plan not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long planId) {
        log.info("Admin deleting subscription plan: {}", planId);
        
        try {
            subscriptionService.deletePlan(planId);
            return ResponseEntity.ok(ApiResponse.success("Plan deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

