package com.app.civify.controller.v1;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@ApiV1Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = subscriptionService.getAllActivePlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<SubscriptionPlan> getPlanById(@PathVariable Long planId) {
        return subscriptionService.getPlanById(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-subscription")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
    public ResponseEntity<UserSubscription> getMyActiveSubscription(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return subscriptionService.getActiveSubscription(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-subscriptions")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
    public ResponseEntity<List<UserSubscription>> getMySubscriptions(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<UserSubscription> subscriptions = subscriptionService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping("/calculate-price")
    public ResponseEntity<Map<String, Object>> calculatePrice(
            @RequestParam Long planId,
            @RequestParam(required = false) String promoCode,
            @RequestParam(defaultValue = "false") boolean isAnnual) {
        
        try {
            BigDecimal totalPrice = subscriptionService.calculateTotalPrice(planId, isAnnual, promoCode);
            
            return ResponseEntity.ok(Map.of(
                    "planId", planId,
                    "isAnnual", isAnnual,
                    "promoCode", promoCode != null ? promoCode : "",
                    "totalPrice", totalPrice
            ));
        } catch (Exception e) {
            log.error("Error calculating price: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/cancel/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
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

    @PostMapping("/renew/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
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

    @GetMapping("/can-use/{feature}")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
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
