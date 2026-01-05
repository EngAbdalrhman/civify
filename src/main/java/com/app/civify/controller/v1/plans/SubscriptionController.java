package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.plan.SimplePlanDTO;
import com.app.civify.dto.subscription.PriceCalculationResponse;
import com.app.civify.dto.subscription.SubscriptionResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.user.SubscriptionUseCase;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@ApiV1Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Subscription management endpoints")
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;

    @Operation(
            summary = "Get all subscription plans",
            description = "Retrieves a list of all active subscription plans available to users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plans retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/plans")
    public ResponseEntity<CommonResponse<List<SimplePlanDTO>>> getAllPlans() {
        List<SimplePlanDTO> plans = subscriptionUseCase.getAllActivePlans();
        return ResponseEntity.ok(CommonResponse.success(plans));
    }
    // maybe in the future we need to hash the id

    @Operation(
            summary = "Get subscription plan by ID",
            description = "Retrieves a specific subscription plan by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/plans/{planId}")
    public ResponseEntity<CommonResponse<SimplePlanDTO>> getPlanById(@PathVariable Long planId) {
        SimplePlanDTO plan = subscriptionUseCase.getPlanById(planId);
        return ResponseEntity.ok(CommonResponse.success(plan));
    }


    @Operation(
            summary = "Calculate subscription price",
            description = "Calculates the total price for a subscription including discounts and promo codes. Use this for checkout page.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price calculated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @GetMapping("/calculate-price")
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<PriceCalculationResponse>> calculatePrice(
            @RequestParam Long planId,
            @RequestParam(defaultValue = "false") boolean isAnnual,
            @RequestParam(required = false) String promoCode,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        PriceCalculationResponse response = subscriptionUseCase.calculatePrice(userId, planId, isAnnual, promoCode);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(
            summary = "Get my active subscription",
            description = "Retrieves the current user's active subscription details.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "No active subscription found",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my-subscription")
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<SubscriptionResponse>> getMyActiveSubscription(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        SubscriptionResponse sub = subscriptionUseCase.getActiveSubscription(userId);
        return ResponseEntity.ok(CommonResponse.success(sub));
    }

    @Operation(
            summary = "Get my subscription history",
            description = "Retrieves all subscriptions (past and present) for the current user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/my-subscriptions")
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<List<SubscriptionResponse>>> getMySubscriptions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        List<SubscriptionResponse> subscriptions = subscriptionUseCase.getUserSubscriptions(userId);
        return ResponseEntity.ok(CommonResponse.success(subscriptions));
    }
// ToDO   not needed

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
    @PostMapping("/cancel")
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<Map<String, String>>> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal.getId();
            subscriptionUseCase.cancelActiveSubscription(userId);
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "message", "Subscription marked for cancellation. It will remain active until the end date."
            )));
        } catch (Exception e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
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
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<Map<String, Object>>> canUseFeature(
            @PathVariable String feature,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal.getId();
            boolean canUse = subscriptionUseCase.canUseFeature(userId, feature);

            return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "feature", feature,
                    "canUse", canUse
            )));
        } catch (Exception e) {
            log.error("Error checking feature access: {}", e.getMessage());
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "canUse", false
            )));
        }
    }
}
