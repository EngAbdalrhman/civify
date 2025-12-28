package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.payment.PaymentInitiationResponse;
import com.app.civify.dto.payment.PaymentVerificationResponse;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.usecase.user.PaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            Long userId = userPrincipal.getId();
            PaymentInitiationResponse response = paymentUseCase.initiatePayment(userId, planId, isAnnual, promoCode, gateway);

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
            @RequestHeader Map<String, String> headers) {
        try {
            log.info("Received payment webhook for gateway {}", gateway);
            log.debug("Payload: {}", payload);

            paymentService.handlePaymentCallback(gateway, null, payload, headers);

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage());
            return ResponseEntity.ok("Webhook received");
        }
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
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "CV.CREATE")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(@PathVariable String paymentId) {
        try {
            PaymentVerificationResponse response = paymentUseCase.verifyAndActivatePayment(paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(PaymentVerificationResponse.builder()
                    .verified(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
