package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.entity.SubscriptionPlan;
import com.app.civify.entity.UserSubscription;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@ApiV1Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Subscription management endpoints")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @Operation(
            summary = "Get all subscription plans",
            description = "Retrieves a list of all active subscription plans available to users."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plans retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class)))
    })
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = subscriptionService.getAllActivePlans();
        return ResponseEntity.ok(plans);
    }

    @Operation(
            summary = "Get subscription plan by ID",
            description = "Retrieves a specific subscription plan by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscriptionPlan.class))),
        @ApiResponse(responseCode = "404", description = "Plan not found",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/plans/{planId}")
    public ResponseEntity<SubscriptionPlan> getPlanById(@PathVariable Long planId) {
        return subscriptionService.getPlanById(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get my active subscription",
            description = "Retrieves the current user's active subscription details.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserSubscription.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "No active subscription found",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my-subscription")
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<UserSubscription> getMyActiveSubscription(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return subscriptionService.getActiveSubscription(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get my subscription history",
            description = "Retrieves all subscriptions (past and present) for the current user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserSubscription.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my-subscriptions")
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<List<UserSubscription>> getMySubscriptions(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<UserSubscription> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @Operation(
            summary = "Cancel subscription",
            description = "Cancels an active subscription. The subscription will remain active until the end of the current billing period.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription cancelled successfully",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Cancellation failed",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/cancel/{subscriptionId}")
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @PathVariable Long subscriptionId,
            Authentication authentication) {

        try {
            subscriptionService.cancelSubscription(subscriptionId);
            return ResponseEntity.ok(Map.of(
                    "message", "Subscription cancelled successfully"
            ));
        } catch (Exception e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Renew subscription",
            description = "Renews an existing subscription for another billing period.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription renewed successfully",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Renewal failed",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/renew/{subscriptionId}")
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<Map<String, String>> renewSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "false") boolean isAnnual,
            Authentication authentication) {

        try {
            subscriptionService.renewSubscription(subscriptionId, isAnnual);
            return ResponseEntity.ok(Map.of(
                    "message", "Subscription renewed successfully"
            ));
        } catch (Exception e) {
            log.error("Error renewing subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Check feature access",
            description = "Checks if the current user can use a specific feature based on their subscription.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feature access checked successfully",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/can-use/{feature}")
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<Map<String, Object>> canUseFeature(
            @PathVariable String feature,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            boolean canUse = subscriptionService.canUserUseFeature(userId, feature);

            return ResponseEntity.ok(Map.of(
                    "feature", feature,
                    "canUse", canUse
            ));
        } catch (Exception e) {
            log.error("Error checking feature access: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "canUse", false
            ));
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        // Extract user ID from authentication principal
        var user = userService.getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
