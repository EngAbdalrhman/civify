package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.entity.plans.TokenPackage;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.plans.TokenPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1Controller
@RequestMapping("/tokens")
@RequiredArgsConstructor
@Tag(name = "Token Packages", description = "Endpoints for managing and purchasing token top-up packages")
public class TokenPackageController {

    private final TokenPackageService tokenPackageService;

    @Operation(
            summary = "Get available token packages",
            description = "Retrieve a list of all active token packages available for purchase",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/packages")
    public ResponseEntity<List<TokenPackage>> getPackages() {
        return ResponseEntity.ok(tokenPackageService.getAllActivePackages());
    }

    @Operation(
            summary = "Create token package (Admin)",
            description = "Create a new token package. Requires CREATE.PLAN permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/packages")
    @CivifyAuthorization(permission = "CREATE.PLAN")
    public ResponseEntity<TokenPackage> createPackage(@RequestBody TokenPackage tokenPackage) {
        return ResponseEntity.ok(tokenPackageService.createPackage(tokenPackage));
    }

    @Operation(
            summary = "Purchase token package",
            description = "Initiate a purchase for a specific token package. Returns payment key/URL.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/purchase/{packageId}")
    public ResponseEntity<PaymentCreationResult> purchasePackage(
            @Parameter(description = "ID of the package to purchase")
            @PathVariable Long packageId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        return ResponseEntity.ok(tokenPackageService.purchasePackage(userId, packageId));
    }
}
