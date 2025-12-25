package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.subscription.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ApiV1Controller
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Admin Subscriptions", description = "Admin endpoints for managing user subscriptions")
@SecurityRequirement(name = "bearer-jwt")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Get all user subscriptions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    @CivifyAuthorization(permission = "SUBSCRIPTION.VIEW")
    public ResponseEntity<Page<UserSubscription>> getAllSubscriptions(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        // Assuming subscriptionService has a method for this, otherwise I'll need to add it.
        // For now, I'll rely on a basic findAll equivalent or create it.
        // Let's assume getAllSubscriptions exists or I will add it to Service.
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(status, pageable));
    }
}
