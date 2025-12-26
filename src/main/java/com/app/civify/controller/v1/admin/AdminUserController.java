package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.service.user.AdminUserService;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Admin controller for user management Provides endpoints for filtering and
 * viewing users
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Admin endpoints for user management and filtering")
// @PreAuthorize("hasAuthority('ROLE_ADMIN')") - Removed in favor of Granular Permissions
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(
            summary = "Filter users with advanced criteria",
            description = "Retrieves users with advanced filtering, sorting, and pagination. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Users filtered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid filter criteria",
                content = @Content(mediaType = "application/json")
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "USER.VIEW")
    @PostMapping("/filter")
    public ResponseEntity<CommonResponse<PagedResponse<UserResponseDTO>>> filterUsers(@Valid @RequestBody InputDataModel inputData) {
        log.info("Admin filtering users with criteria: {}", inputData);

        PagedResponse<UserResponseDTO> pagedResponse = adminUserService.filterUsers(inputData);

        return ResponseEntity.ok(CommonResponse.success("Users filtered successfully", pagedResponse));
    }

    @Operation(
            summary = "Get all users with pagination",
            description = "Retrieves all users with pagination (no filtering). Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Users retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "USER.VIEW")
    @GetMapping
    public ResponseEntity<CommonResponse<PagedResponse<UserResponseDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Admin fetching all users - page: {}, size: {}", page, size);

        PagedResponse<UserResponseDTO> pagedResponse = adminUserService.getAllUsers(page, size);

        return ResponseEntity.ok(CommonResponse.success("Users retrieved successfully", pagedResponse));
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by ID with optional statistics. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "USER.VIEW")
    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponseDTO>> getUserById(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean includeStats) {
        log.info("Admin fetching user with ID: {} (includeStats: {})", userId, includeStats);

        UserResponseDTO userDTO = includeStats
                ? adminUserService.getUserByIdWithStats(userId)
                : adminUserService.getUserById(userId);

        return ResponseEntity.ok(CommonResponse.success("User retrieved successfully", userDTO));
    }
    @Operation(
            summary = "Update user limits",
            description = "Updates custom limits (CV and API) for a user. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "User limits updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "USER.MODIFY")
    @PutMapping("/{userId}/limits")
    public ResponseEntity<CommonResponse<UserResponseDTO>> updateUserLimits(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer cvLimit,
            @RequestParam(required = false) Integer apiLimit) {
        log.info("Admin updating limits for user {}: CV={}, API={}", userId, cvLimit, apiLimit);

        UserResponseDTO userDTO = adminUserService.updateUserLimits(userId, cvLimit, apiLimit);

        return ResponseEntity.ok(CommonResponse.success("User limits updated successfully", userDTO));
    }
}
