package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.dto.token.TokenBalanceResponse;
import com.app.civify.entity.plans.TokenPackage;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.plans.TokenPackageService;
import com.app.civify.util.security.EntityType;
import com.app.civify.util.security.IdEncodingUtil;
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
    private final IdEncodingUtil idEncodingUtil;

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
            @Parameter(description = "Encoded ID of the package to purchase")
            @PathVariable String packageId,
            @RequestParam(required = false) String promoCode,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        Long decodedPackageId = idEncodingUtil.decode(packageId, EntityType.TOKEN_PACKAGE);
        return ResponseEntity.ok(tokenPackageService.purchasePackage(userId, decodedPackageId, promoCode));
    }

    @Operation(
            summary = "Preview token purchase",
            description = "Preview purchase details including promo code discount without initiating transaction.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/purchase/{packageId}/preview")
    public ResponseEntity<com.app.civify.dto.payment.PurchasePreviewResponse> previewPurchase(
            @Parameter(description = "Encoded ID of the package")
            @PathVariable String packageId,
            @RequestParam(required = false) String promoCode,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        Long decodedPackageId = idEncodingUtil.decode(packageId, EntityType.TOKEN_PACKAGE);
        return ResponseEntity.ok(tokenPackageService.previewTokenPurchase(userId, decodedPackageId, promoCode));
    }

    // ============ ADMIN ENDPOINTS ============

    @Operation(
            summary = "List all token packages (Admin)",
            description = "Retrieve all token packages including inactive ones. Requires TOKEN_PACKAGE.VIEW permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/packages/all")
    @CivifyAuthorization(permission = "TOKEN_PACKAGE.VIEW")
    public ResponseEntity<List<TokenPackage>> getAllPackages() {
        return ResponseEntity.ok(tokenPackageService.getAllPackages());
    }

    @Operation(
            summary = "Get token package by ID (Admin)",
            description = "Retrieve a specific token package by ID. Requires TOKEN_PACKAGE.VIEW permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/packages/{id}")
    @CivifyAuthorization(permission = "TOKEN_PACKAGE.VIEW")
    public ResponseEntity<TokenPackage> getPackageById(
            @Parameter(description = "Encoded ID of the package") @PathVariable String id) {
        Long decodedId = idEncodingUtil.decode(id, EntityType.TOKEN_PACKAGE);
        return ResponseEntity.ok(tokenPackageService.getPackageById(decodedId));
    }

    @Operation(
            summary = "Create token package (Admin)",
            description = "Create a new token package. Requires TOKEN_PACKAGE.MODIFY permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/packages")
    @CivifyAuthorization(permission = "TOKEN_PACKAGE.MODIFY")
    public ResponseEntity<TokenPackage> createPackage(@RequestBody TokenPackage tokenPackage) {
        return ResponseEntity.ok(tokenPackageService.createPackage(tokenPackage));
    }

    @Operation(
            summary = "Update token package (Admin)",
            description = "Update an existing token package. Requires TOKEN_PACKAGE.MODIFY permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PutMapping("/packages/{id}")
    @CivifyAuthorization(permission = "TOKEN_PACKAGE.MODIFY")
    public ResponseEntity<TokenPackage> updatePackage(
            @Parameter(description = "Encoded ID of the package to update") @PathVariable String id,
            @RequestBody TokenPackage tokenPackage) {
        Long decodedId = idEncodingUtil.decode(id, EntityType.TOKEN_PACKAGE);
        return ResponseEntity.ok(tokenPackageService.updatePackage(decodedId, tokenPackage));
    }

    @Operation(
            summary = "Deactivate token package (Admin)",
            description = "Deactivate (soft delete) a token package. Requires TOKEN_PACKAGE.MODIFY permission.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @DeleteMapping("/packages/{id}")
    @CivifyAuthorization(permission = "TOKEN_PACKAGE.MODIFY")
    public ResponseEntity<Void> deactivatePackage(
            @Parameter(description = "Encoded ID of the package to deactivate") @PathVariable String id) {
        Long decodedId = idEncodingUtil.decode(id, EntityType.TOKEN_PACKAGE);
        tokenPackageService.deactivatePackage(decodedId);
        return ResponseEntity.noContent().build();
    }
}
