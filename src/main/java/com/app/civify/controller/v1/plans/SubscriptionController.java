package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.subscription.SubscriptionResponse;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.payment.PromoCodeService;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.service.user.UserService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApiV1Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Subscription management endpoints")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final PromoCodeService promoCodeService;

    @Operation(
            summary = "Get all subscription plans",
            description = "Retrieves a list of all active subscription plans available to users."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plans retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class)))
    })
    @GetMapping("/plans")
    public ResponseEntity<CommonResponse<List<SubscriptionPlan>>> getAllPlans() {
        List<SubscriptionPlan> plans = subscriptionService.getAllActivePlans();
        return ResponseEntity.ok(CommonResponse.success(plans));
    }

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
    public ResponseEntity<CommonResponse<SubscriptionPlan>> getPlanById(@PathVariable Long planId) {
        SubscriptionPlan plan = subscriptionService.getPlanById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription Plan not found with id: " + planId));
        return ResponseEntity.ok(CommonResponse.success(plan));
    }

    @Operation(
            summary = "Validate promo code",
            description = "Validates a promo code for checkout and returns discount information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promo code validated"),
            @ApiResponse(responseCode = "400", description = "Invalid promo code")
    })
    @PostMapping("/validate-promo")
    public ResponseEntity<CommonResponse<Map<String, Object>>> validatePromoCode(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        Number amountNum = (Number) request.get("amount");
        BigDecimal amount = amountNum != null ? BigDecimal.valueOf(amountNum.doubleValue()) : BigDecimal.ZERO;
        
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(CommonResponse.error("Promo code is required"));
        }
        
        Optional<PromoCode> promoOpt = promoCodeService.validatePromoCode(code, amount);
        
        if (promoOpt.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.success("Invalid or expired promo code", Map.of(
                    "valid", false,
                    "message", "Invalid or expired promo code"
            )));
        }
        
        PromoCode promo = promoOpt.get();
        BigDecimal discountAmount = promo.calculateDiscount(amount);
        
        return ResponseEntity.ok(CommonResponse.success(Map.of(
                "valid", true,
                "discountType", promo.getDiscountType().name(),
                "discountValue", promo.getDiscountValue(),
                "discountAmount", discountAmount,
                "message", "Promo code applied successfully"
        )));
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
        UserSubscription sub = subscriptionService.getActiveSubscription(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found"));

        return ResponseEntity.ok(CommonResponse.success(mapToResponse(sub)));
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
        List<UserSubscription> subscriptions = subscriptionService.getUserSubscriptions(userId);
        
        List<SubscriptionResponse> response = subscriptions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(CommonResponse.success(response));
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
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<Map<String, String>>> cancelSubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            subscriptionService.cancelSubscription(subscriptionId);
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "message", "Subscription cancelled successfully"
            )));
        } catch (Exception e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
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
    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<CommonResponse<Map<String, String>>> renewSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "false") boolean isAnnual,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            subscriptionService.renewSubscription(subscriptionId, isAnnual);
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "message", "Subscription renewed successfully"
            )));
        } catch (Exception e) {
            log.error("Error renewing subscription: {}", e.getMessage());
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
            boolean canUse = subscriptionService.canUserUseFeature(userId, feature);

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

    private SubscriptionResponse mapToResponse(UserSubscription sub) {
        if (sub == null) return null;
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .userId(sub.getUser() != null ? sub.getUser().getId() : null)
                .planId(sub.getPlan() != null ? sub.getPlan().getId() : null)
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : null)
                .planDescription(sub.getPlan() != null ? sub.getPlan().getDescription() : null)
                .cvLimit(sub.getPlan() != null ? sub.getPlan().getCvLimit() : null)
                .apiLimit(sub.getPlan() != null ? sub.getPlan().getApiLimit() : null)
                .currentCvCount(sub.getCurrentCvCount())
                .currentApiCount(sub.getCurrentApiCount())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .paymentTransactionId(sub.getPaymentTransactionId())
                .usageResetDate(sub.getUsageResetDate())
                .currentTokenUsage(sub.getCurrentTokenUsage())
                .build();
    }
}
