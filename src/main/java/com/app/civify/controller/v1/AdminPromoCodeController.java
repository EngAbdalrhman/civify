package com.app.civify.controller.v1;

import com.app.civify.entity.PromoCode;
import com.app.civify.service.payment.PromoCodeService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@ApiV1Controller
@RequestMapping("/admin/promocodes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Promo Codes", description = "Admin endpoints for managing promo codes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {

    private final PromoCodeService promoCodeService;

    @Operation(
            summary = "Filter promo codes",
            description = "Retrieves a paginated list of promo codes based on filter criteria.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promo codes retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponse.class)))
    })
    @PostMapping("/filter")
    public ResponseEntity<CommonResponse<PagedResponse<PromoCode>>> filterPromoCodes(@RequestBody InputDataModel inputData) {
        return ResponseEntity.ok(CommonResponse.success(promoCodeService.filterPromoCodes(inputData)));
    }

    @Operation(
            summary = "Create promo code",
            description = "Creates a new promo code.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promo code created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromoCode.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<CommonResponse<PromoCode>> createPromoCode(@RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(CommonResponse.success(promoCodeService.createPromoCode(promoCode)));
    }

    @Operation(
            summary = "Update promo code",
            description = "Updates an existing promo code.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promo code updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromoCode.class))),
            @ApiResponse(responseCode = "404", description = "Promo code not found",
                    content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<PromoCode>> updatePromoCode(@PathVariable Long id, @RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(CommonResponse.success(promoCodeService.updatePromoCode(id, promoCode)));
    }

    @Operation(
            summary = "Deactivate promo code",
            description = "Deactivates a promo code.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promo code deactivated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PromoCode.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Promo code not found",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<CommonResponse<PromoCode>> deactivatePromoCode(@PathVariable Long id) {
        return ResponseEntity.ok(CommonResponse.success(promoCodeService.deactivatePromoCode(id)));
    }

    @Operation(
            summary = "Delete promo code",
            description = "Soft deletes a promo code.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promo code deleted successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Promo code not found",
                    content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deletePromoCode(@PathVariable Long id) {
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.ok(CommonResponse.success("Promo code deleted successfully", null));
    }
}
