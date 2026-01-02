package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.plan.PlanCreateRequest;
import com.app.civify.dto.plan.PlanDetailResponse;
import com.app.civify.dto.plan.PlanUpdateRequest;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.usecase.admin.AdminPlanUseCase;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for managing subscription plans All endpoints require ADMIN
 * role
 */
@ApiV1Controller
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Plan Management", description = "Admin endpoints for managing subscription plans")
public class AdminPlanController {

    private final AdminPlanUseCase adminPlanUseCase;

    @Operation(
            summary = "Filter plans with advanced criteria",
            description = "Retrieves plans with advanced filtering, sorting, and pagination. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plans filtered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid filter criteria",
                content = @Content(mediaType = "application/json")
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.VIEW")
    @PostMapping("/filter")
    public ResponseEntity<CommonResponse<PagedResponse<SubscriptionPlan>>> filterPlans(@Valid @RequestBody com.app.civify.util.filter.InputDataModel inputData) {
        log.info("Admin filtering plans with criteria: {}", inputData);

        PagedResponse<SubscriptionPlan> pagedResponse = adminPlanUseCase.filterPlans(inputData);

        return ResponseEntity.ok(CommonResponse.success("Plans filtered successfully", pagedResponse));
    }

    @Operation(
            summary = "Get all subscription plans (including inactive)",
            description = "Retrieves all subscription plans including inactive ones with full details. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Plans retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin access required",
                    content = @Content(mediaType = "application/json")
            )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.VIEW")
    @GetMapping
    public ResponseEntity<List<PlanDetailResponse>> getAllPlans() {
        log.info("Admin fetching all subscription plans");
        List<PlanDetailResponse> response = adminPlanUseCase.getAllPlans();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get plan lookup list",
            description = "Retrieves simple plan list (id, name) for filter dropdowns. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan lookup list retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.app.civify.dto.plan.PlanLookupDto.class))
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.VIEW")
    @GetMapping("/lookup")
    public ResponseEntity<List<com.app.civify.dto.plan.PlanLookupDto>> getPlanLookup() {
        log.info("Fetching plan lookup list for filters");
        List<com.app.civify.dto.plan.PlanLookupDto> lookup = adminPlanUseCase.getAllPlans().stream()
                .map(plan -> com.app.civify.dto.plan.PlanLookupDto.builder()
                        .id(plan.getId())
                        .name(plan.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(lookup);
    }

    @Operation(
            summary = "Get plan with subscription statistics",
            description = "Retrieves a plan with full details and active subscription count. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Plan retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanDetailResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plan not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.VIEW")
    @GetMapping("/{planId}")
    public ResponseEntity<PlanDetailResponse> getPlanWithStats(@PathVariable Long planId) {
        log.info("Admin fetching plan {} with statistics", planId);
        return ResponseEntity.ok(adminPlanUseCase.getPlanWithStats(planId));
    }

    @Operation(
            summary = "Create a new subscription plan",
            description = "Creates a new subscription plan with features, pricing, discounts, and integrations. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Plan created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanDetailResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid plan data",
                content = @Content(mediaType = "application/json")
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.MODIFY")
    @PostMapping
    public ResponseEntity<CommonResponse<PlanDetailResponse>> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        log.info("Admin creating new subscription plan: {}", request.getName());

        try {
            PlanDetailResponse response = adminPlanUseCase.createPlan(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(CommonResponse.success("Plan created successfully", response));
        } catch (Exception e) {
            log.error("Error creating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(CommonResponse.error(e.getMessage()));
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
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanDetailResponse.class))
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
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.MODIFY")
    @PutMapping("/{planId}")
    public ResponseEntity<CommonResponse<PlanDetailResponse>> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody PlanUpdateRequest request) {
        log.info("Admin updating subscription plan: {}", planId);

        try {
            PlanDetailResponse response = adminPlanUseCase.updatePlan(planId, request);
            return ResponseEntity.ok(CommonResponse.success("Plan updated successfully", response));
        } catch (Exception e) {
            log.error("Error updating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(CommonResponse.error(e.getMessage()));
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
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.MODIFY")
    @PatchMapping("/{planId}/activate")
    public ResponseEntity<CommonResponse<SubscriptionPlan>> activatePlan(@PathVariable Long planId) {
        log.info("Admin activating subscription plan: {}", planId);

        try {
            SubscriptionPlan activated = adminPlanUseCase.activatePlan(planId);
            return ResponseEntity.ok(CommonResponse.success("Plan activated successfully", activated));
        } catch (Exception e) {
            log.error("Error activating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(CommonResponse.error(e.getMessage()));
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
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.MODIFY")
    @PatchMapping("/{planId}/deactivate")
    public ResponseEntity<CommonResponse<SubscriptionPlan>> deactivatePlan(@PathVariable Long planId) {
        log.info("Admin deactivating subscription plan: {}", planId);

        try {
            SubscriptionPlan deactivated = adminPlanUseCase.deactivatePlan(planId);
            return ResponseEntity.ok(CommonResponse.success("Plan deactivated successfully", deactivated));
        } catch (Exception e) {
            log.error("Error deactivating plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(CommonResponse.error(e.getMessage()));
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
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "PLAN.MODIFY")
    @DeleteMapping("/{planId}")
    public ResponseEntity<CommonResponse<Void>> deletePlan(@PathVariable Long planId) {
        log.info("Admin deleting subscription plan: {}", planId);

        try {
            adminPlanUseCase.deletePlan(planId);
            return ResponseEntity.ok(CommonResponse.success("Plan deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting plan: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(CommonResponse.error(e.getMessage()));
        }
    }
}
