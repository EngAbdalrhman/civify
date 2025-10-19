package com.app.civify.controller.v1;

import com.app.civify.repository.UserRepository;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@ApiV1Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing and verification endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @Operation(
            summary = "Initiate payment",
            description = "Initiates a payment process for a subscription plan using Paymob gateway.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment initiated successfully",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Payment initiation failed",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @RequestParam Long planId,
            @RequestParam(required = false) String promoCode,
            @RequestParam(defaultValue = "false") boolean isAnnual,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            String paymentKey = subscriptionService.initiatePayment(userId, planId, isAnnual, promoCode);

            return ResponseEntity.ok(Map.of(
                    "paymentKey", paymentKey,
                    "message", "Payment initiated successfully"
            ));
        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Handle payment webhook",
            description = "Processes payment webhooks from Paymob gateway. This endpoint is public and called automatically by Paymob."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    })
    @PostMapping("/webhook")
    public ResponseEntity<String> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received payment webhook: {}", payload);

            String orderId = (String) payload.get("order_id");
            String success = (String) payload.get("success");

            if ("true".equals(success)) {
                // Find the payment transaction and activate subscription
                String paymentKey = (String) payload.get("payment_key");
                subscriptionService.activateSubscription(paymentKey);
            }

            paymentService.handlePaymentCallback(orderId, payload);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage());
            return ResponseEntity.ok("Webhook received"); // Return 200 to avoid retries
        }
    }

    @Operation(
            summary = "Verify payment",
            description = "Verifies the status of a payment and activates the subscription if successful.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment verification completed",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Payment verification failed",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/verify/{paymentId}")
    @PreAuthorize("hasAnyRole('USER', 'LICENSED_USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyPayment(@PathVariable String paymentId) {
        try {
            boolean isVerified = paymentService.verifyPayment(paymentId);

            if (isVerified) {
                subscriptionService.activateSubscription(paymentId);
            }

            return ResponseEntity.ok(Map.of(
                    "verified", isVerified,
                    "message", isVerified ? "Payment verified successfully" : "Payment verification failed"
            ));
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        String username = authentication.getName();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId();
    }
}
