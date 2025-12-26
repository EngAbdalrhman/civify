package com.app.civify.controller.v1.plans;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.admin.AdminSubscriptionUseCase;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.dto.subscription.UserSubscriptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ApiV1Controller
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Admin Subscriptions", description = "Admin endpoints for managing user subscriptions")
@SecurityRequirement(name = "bearer-jwt")
public class AdminSubscriptionController {

    private final AdminSubscriptionUseCase adminSubscriptionUseCase;

    @Operation(summary = "Filter subscriptions with advanced criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions filtered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class)))
    })
    @PostMapping("/filter")
    @CivifyAuthorization(permission = "SUBSCRIPTION.VIEW")
    public ResponseEntity<CommonResponse<PagedResponse<UserSubscriptionDto>>> filterSubscriptions(
            @RequestBody InputDataModel inputData) {
        PagedResponse<UserSubscriptionDto> response = adminSubscriptionUseCase.filterSubscriptions(inputData);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
