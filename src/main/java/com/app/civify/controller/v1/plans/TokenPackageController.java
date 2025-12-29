package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.token.TokenBalanceResponse;
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

    // ============ USER ENDPOINTS ============

    @Operation(
            summary = "Get available token packages",
            description = "Retrieve a list of all active token packages available for purchase"
    )
    @GetMapping("/packages")
    public ResponseEntity<List<TokenPackage>> getPackages() {
        return ResponseEntity.ok(tokenPackageService.getAllActivePackages());
    }

    @Operation(
            summary = "Get user's token balance",
            description = "Retrieve the current user's token wallet balance (purchased tokens)",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/balance")
    public ResponseEntity<TokenBalanceResponse> getBalance(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        return ResponseEntity.ok(tokenPackageService.getUserTokenBalance(userId));
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

    // ============ ADMIN ENDPOINTS ============

    @Operation(
            summary = "List all token packages (Admin)",
            description = "Retrieve all token packages including inactive ones. Requires MANAGE.PLAN permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/packages/all")
    @CivifyAuthorization(permission = "MANAGE.PLAN")
    public ResponseEntity<List<TokenPackage>> getAllPackages() {
        return ResponseEntity.ok(tokenPackageService.getAllPackages());
    }

    @Operation(
            summary = "Get token package by ID (Admin)",
            description = "Retrieve a specific token package by ID. Requires MANAGE.PLAN permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/packages/{id}")
    @CivifyAuthorization(permission = "MANAGE.PLAN")
    public ResponseEntity<TokenPackage> getPackageById(
            @Parameter(description = "ID of the package") @PathVariable Long id) {
        return ResponseEntity.ok(tokenPackageService.getPackageById(id));
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
            summary = "Update token package (Admin)",
            description = "Update an existing token package. Requires MANAGE.PLAN permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PutMapping("/packages/{id}")
    @CivifyAuthorization(permission = "MANAGE.PLAN")
    public ResponseEntity<TokenPackage> updatePackage(
            @Parameter(description = "ID of the package to update") @PathVariable Long id,
            @RequestBody TokenPackage tokenPackage) {
        return ResponseEntity.ok(tokenPackageService.updatePackage(id, tokenPackage));
    }

    @Operation(
            summary = "Deactivate token package (Admin)",
            description = "Deactivate (soft delete) a token package. Requires MANAGE.PLAN permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @DeleteMapping("/packages/{id}")
    @CivifyAuthorization(permission = "MANAGE.PLAN")
    public ResponseEntity<Void> deactivatePackage(
            @Parameter(description = "ID of the package to deactivate") @PathVariable Long id) {
        tokenPackageService.deactivatePackage(id);
        return ResponseEntity.noContent().build();
    }
}
