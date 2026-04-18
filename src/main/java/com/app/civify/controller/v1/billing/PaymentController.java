package com.app.civify.controller.v1.billing;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.dto.payment.PaymentVerificationResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.usecase.user.PaymentUseCase;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@ApiV1Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing and verification endpoints")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;
    private final PaymentService paymentService;

    @Value("${payment.redirect.success-url:/payment/success}")
    private String successRedirectUrl;

    @Value("${payment.redirect.fail-url:/payment/failure}")
    private String failRedirectUrl;

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
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @RequestParam Long planId,
            @RequestParam(required = false) String promoCode,
            @RequestParam(defaultValue = "false") boolean isAnnual,
            @RequestParam(required = false) String gateway,
            @RequestParam(required = false) PaymentMethods paymentMethod,
            @RequestParam(required = false) String phoneNumber,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal.getId();
            PaymentInitiationResponse response = paymentUseCase.initiatePayment(userId, planId, isAnnual, promoCode, gateway, paymentMethod, phoneNumber);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(PaymentInitiationResponse.builder()
                    .message(e.getMessage())
                    .build());
        }
    }

    @Operation(
            summary = "Handle payment webhook",
            description = "Processes payment webhooks from payment gateways. This endpoint is public and called automatically by the gateway."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    })
    @PostMapping("/webhook/{gateway}")
    public ResponseEntity<String> handlePaymentWebhook(
            @PathVariable String gateway,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            @RequestParam Map<String, String> queryParams) {
        try {
            log.info("Received payment webhook for gateway {}", gateway);
            log.debug("Payload: {}", payload);

            // Create mutable map combining headers and query params
            // Paymob sends HMAC in query params, so we merge them
            Map<String, String> combinedHeaders = new HashMap<>(headers);
            if (queryParams != null && !queryParams.isEmpty()) {
                combinedHeaders.putAll(queryParams);
            }

            paymentService.handlePaymentCallback(gateway, null, payload, combinedHeaders);

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage());
            return ResponseEntity.ok("Webhook received");
        }
    }

    @Operation(
        summary = "Handle payment redirect (GET)",
        description = "Handles browser redirects from payment gateways."
    )
    @GetMapping("/webhook/{gateway}")
    public void handlePaymentRedirect(
            @PathVariable String gateway,
            @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException {
        
        // Determine redirect URL based on payment params directly (uses gateway-specific logic)
        boolean paymentSuccess = paymentService.isRedirectSuccess(gateway, params);
        String baseRedirectUrl = paymentSuccess ? successRedirectUrl : failRedirectUrl;
        
        // Build the final URL with query parameters
        // We must forward the gateway params so the frontend can parse them (e.g. transaction ID)
        // We also add the 'gateway' param so frontend knows which logic to use
        UriComponentsBuilder uriBuilder = 
            UriComponentsBuilder.fromUriString(baseRedirectUrl)
            .queryParam("gateway", gateway);
            
        // Add all existing params
        params.forEach(uriBuilder::queryParam);
        
        String finalRedirectUrl = uriBuilder.build().toUriString();
        
        try {
            log.info("Received payment redirect for gateway {}", gateway);
            paymentService.handlePaymentRedirect(gateway, params);
            // Processing succeeded - redirect already determined by payment params
        } catch (Exception e) {
            // Log the error but don't change redirect decision
            // The payment outcome is determined by gateway params, not our processing
            log.error("Error processing payment redirect (payment status from gateway: {}): {}", 
                    paymentSuccess ? "SUCCESS" : "FAILED", e.getMessage());
        }
        
        response.sendRedirect(finalRedirectUrl);
    }

    @Operation(
            summary = "Get enabled gateways",
            description = "Returns a list of enabled payment gateways."
    )
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPaymentConfig() {
        return ResponseEntity.ok(Map.of(
                "enabledGateways", paymentUseCase.getEnabledGateways()
        ));
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
//    @CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @PathVariable String paymentId,
            @RequestParam(required = false) String gateway) {
        try {
            PaymentVerificationResponse response = paymentUseCase.verifyAndActivatePayment(paymentId, gateway);
            return ResponseEntity.ok(response);
        } catch (ResourceAccessException e) {
            log.warn("Infrastructure error verifying payment {}: {}", paymentId, e.getMessage());
            // Infrastructure error (API timeout, Paymob unavailable) — return 503 with retryable flag
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(PaymentVerificationResponse.builder()
                    .verified(false)
                    .retryable(true)
                    .message("Payment verification temporarily unavailable. Please try again.")
                    .build());
        } catch (Exception e) {
            log.error("Error verifying payment {}: {}", paymentId, e.getMessage());
            // Business logic failure (e.g., subscription not found) — return 200 with verified=false
            return ResponseEntity.ok(PaymentVerificationResponse.builder()
                    .verified(false)
                    .retryable(false)
                    .message("Payment verification failed: " + e.getMessage())
                    .build());
        }
    }
}
