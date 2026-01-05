package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.user.UserResponseDTO;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.admin.AdminUserUseCase;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
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
public class AdminUserController {

    private final AdminUserUseCase adminUserUseCase;

    @Operation(
            summary = "Filter users with advanced criteria",
            description = "Retrieves users with advanced filtering, sorting, and pagination. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Users filtered successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid filter criteria",
                content = @Content(mediaType = "application/json")
        )
    })
    @CivifyAuthorization(permission = "USER.VIEW")
    @PostMapping("/filter")
    public ResponseEntity<CommonResponse<PagedResponse<UserResponseDTO>>> filterUsers(@Valid @RequestBody InputDataModel inputData) {
        log.info("Admin filtering users with criteria: {}", inputData);

        PagedResponse<UserResponseDTO> pagedResponse = adminUserUseCase.filterUsers(inputData);

        return ResponseEntity.ok(CommonResponse.success("Users filtered successfully", pagedResponse));
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user by ID with optional statistics. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "User retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @CivifyAuthorization(permission = "USER.VIEW")
    @GetMapping("/{userId}")
    public ResponseEntity<CommonResponse<UserResponseDTO>> getUserById(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean includeStats) {
        log.info("Admin fetching user with ID: {} (includeStats: {})", userId, includeStats);

        UserResponseDTO userDTO = includeStats
                ? adminUserUseCase.getUserByIdWithStats(userId)
                : adminUserUseCase.getUserById(userId);

        return ResponseEntity.ok(CommonResponse.success("User retrieved successfully", userDTO));
    }
    @Operation(
            summary = "Update user limits",
            description = "Updates custom CV limit for a user. Admin only.",
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
    @CivifyAuthorization(permission = "USER.MODIFY")
    @PutMapping("/{userId}/limits")
    public ResponseEntity<CommonResponse<UserResponseDTO>> updateUserLimits(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer cvLimit) {
        log.info("Admin updating limits for user {}: CV={}", userId, cvLimit);

        UserResponseDTO userDTO = adminUserUseCase.updateUserLimits(userId, cvLimit);

        return ResponseEntity.ok(CommonResponse.success("User limits updated successfully", userDTO));
    }

    @Operation(
            summary = "Toggle user account lock",
            description = "Toggles the account lock status (ban/unban) for a user. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Account lock status toggled successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @CivifyAuthorization(permission = "USER.MODIFY")
    @PatchMapping("/{userId}/toggle-lock")
    public ResponseEntity<CommonResponse<UserResponseDTO>> toggleAccountLock(@PathVariable Long userId) {
        log.info("Admin toggling account lock for user {}", userId);

        UserResponseDTO userDTO = adminUserUseCase.toggleAccountLock(userId);

        return ResponseEntity.ok(CommonResponse.success("Account lock status toggled successfully", userDTO));
    }

    @Operation(
            summary = "Update user group",
            description = "Assigns a user to a permission group. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "User group updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "User or group not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @CivifyAuthorization(permission = "USER.MODIFY_GROUP")
    @PatchMapping("/{userId}/group")
    public ResponseEntity<CommonResponse<UserResponseDTO>> updateUserGroup(
            @PathVariable Long userId,
            @RequestParam Long groupId) {
        log.info("Admin updating group for user {}: groupId={}", userId, groupId);

        UserResponseDTO userDTO = adminUserUseCase.updateUserGroup(userId, groupId);

        return ResponseEntity.ok(CommonResponse.success("User group updated successfully", userDTO));
    }

    @Operation(
            summary = "Update user tokens",
            description = "Updates extra tokens (wallet) for a user. Admin only.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "User tokens updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(mediaType = "application/json")
        )
    })
    @CivifyAuthorization(permission = "USER.MODIFY_TOKENS")
    @PatchMapping("/{userId}/tokens")
    public ResponseEntity<CommonResponse<UserResponseDTO>> updateUserTokens(
            @PathVariable Long userId,
            @RequestParam Integer extraTokens) {
        log.info("Admin updating tokens for user {}: extraTokens={}", userId, extraTokens);

        UserResponseDTO userDTO = adminUserUseCase.updateUserTokens(userId, extraTokens);

        return ResponseEntity.ok(CommonResponse.success("User tokens updated successfully", userDTO));
    }
}
