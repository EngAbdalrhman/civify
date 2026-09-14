package com.app.civify.controller.external;

import com.app.civify.annotation.RequiresScope;
import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.controller.v1.billing.PayPerCvController;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.payment.PayPerCvPricingDto;
import com.app.civify.dto.payment.PayPerCvPurchaseRequest;
import com.app.civify.security.UserPrincipal;
import com.app.civify.util.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@ApiV1Controller
@RequestMapping("/external/pay-per-cv")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "External Pay-Per-CV API", description = "Endpoints for AI agents to check pricing and purchase one-time CV passes")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ExternalPayPerCvController {

    private final PayPerCvController payPerCvController;

    @Operation(summary = "Get Pay-Per-CV localized pricing (External)")
    @GetMapping("/pricing")
    public ResponseEntity<CommonResponse<List<PayPerCvPricingDto>>> getPricing(HttpServletRequest request) {
        return payPerCvController.getPricing(request);
    }

    @Operation(
            summary = "Purchase Pay-Per-CV pass (External)",
            description = "Initiates checkout for a one-time CV pass. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("billing:purchase")
    @PostMapping("/purchase")
    public ResponseEntity<CommonResponse<PaymentCreationResult>> purchase(
            @RequestBody PayPerCvPurchaseRequest purchaseRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {
        return payPerCvController.purchase(purchaseRequest, userPrincipal, request);
    }

    @Operation(
            summary = "Get entitlement status for a specific resume (External)",
            description = "Checks whether a CV has an active 30-day unwatermarked pass. Requires X-API-KEY header.",
            security = @SecurityRequirement(name = "api-key")
    )
    @RequiresScope("billing:read")
    @GetMapping("/status")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getStatus(
            @RequestParam String resumeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return payPerCvController.getStatus(resumeId, userPrincipal);
    }
}
