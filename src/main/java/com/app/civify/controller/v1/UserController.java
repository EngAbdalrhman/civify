package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.user.ProfileUpdateRequest;
import com.app.civify.dto.user.UserPreferenceDTO;
import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.user.UserPreferenceService;
import com.app.civify.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.app.civify.dto.user.UserDto;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@ApiV1Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final UserPreferenceService userPreferenceService;

    @Operation(
            summary = "Get current user profile",
            description = "Retrieves the profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(userService.getMyUser());
    }

    @Operation(
            summary = "Update current user profile",
            description = "Updates the profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(@RequestBody ProfileUpdateRequest request, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.updateMyUser(request, userPrincipal));
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all users. Requires admin or licensed user role.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping
    @com.app.civify.security.annotation.CivifyAuthorization(permission = "USER.VIEW")
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(
            summary = "Get user preferences",
            description = "Retrieves the preferences (language, theme) of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserPreferenceDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferenceDTO> getMyPreferences(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userPreferenceService.getUserPreferences(userPrincipal.getId()));
    }

    @Operation(
            summary = "Update user preferences",
            description = "Updates the preferences (language, theme) of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserPreferenceDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/me/preferences")
    public ResponseEntity<UserPreferenceDTO> updateMyPreferences(@RequestBody UserPreferenceDTO preferences,
                                                                 @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userPreferenceService.updatePreferences(userPrincipal.getId(), preferences));
    }
}

