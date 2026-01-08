package com.app.civify.controller.v1.security;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.group.GroupCreateRequest;
import com.app.civify.dto.group.GroupResponse;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.usecase.admin.AdminGroupUseCase;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for managing user groups and permissions
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/groups")
@RequiredArgsConstructor
@Tag(name = "Admin Group Management", description = "Admin endpoints for managing groups and permissions")
public class AdminGroupController {

    private final AdminGroupUseCase adminGroupUseCase;

    @Operation(
            summary = "Filter groups with advanced criteria",
            description = "Retrieves groups with filtering, sorting, and pagination",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.VIEW")
    @PostMapping("/filter")
    public ResponseEntity<CommonResponse<PagedResponse<GroupResponse>>> filterGroups(
            @Valid @RequestBody InputDataModel inputData) {
        log.info("Admin filtering groups with criteria: {}", inputData);

        PagedResponse<GroupResponse> response = adminGroupUseCase.filterGroups(inputData);

        return ResponseEntity.ok(CommonResponse.success("Groups filtered successfully", response));
    }

    @Operation(
            summary = "Get all groups",
            description = "Retrieves all groups with their permissions",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.VIEW")
    @GetMapping
    public ResponseEntity<CommonResponse<List<GroupResponse>>> getAllGroups() {
        log.info("Admin fetching all groups");

        List<GroupResponse> responses = adminGroupUseCase.getAllGroups();

        return ResponseEntity.ok(CommonResponse.success("Groups retrieved successfully", responses));
    }

    @Operation(
            summary = "Get all permissions",
            description = "Retrieves all available permissions for assignment",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.VIEW")
    @GetMapping("/permissions")
    public ResponseEntity<CommonResponse<List<GroupResponse.PermissionDto>>> getAllPermissions() {
        log.info("Admin fetching all permissions");
        
        List<GroupResponse.PermissionDto> permissions = adminGroupUseCase.getAllPermissions();

        return ResponseEntity.ok(CommonResponse.success("Permissions retrieved successfully", permissions));
    }

    @Operation(
            summary = "Get group by ID",
            description = "Retrieves a specific group with its permissions",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.VIEW")
    @GetMapping("/{groupId}")
    public ResponseEntity<CommonResponse<GroupResponse>> getGroupById(@PathVariable Long groupId) {
        log.info("Admin fetching group with ID: {}", groupId);

        GroupResponse response = adminGroupUseCase.getGroupById(groupId);

        return ResponseEntity.ok(CommonResponse.success("Group retrieved successfully", response));
    }

    @Operation(
            summary = "Create new group",
            description = "Creates a new group with specified permissions",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.MODIFY")
    @PostMapping
    public ResponseEntity<CommonResponse<GroupResponse>> createGroup(
            @Valid @RequestBody GroupCreateRequest request) {
        log.info("Admin creating group: {}", request.getName());

        GroupResponse response = adminGroupUseCase.createGroup(request);

        return ResponseEntity.ok(CommonResponse.success("Group created successfully", response));
    }

    @Operation(
            summary = "Update group permissions",
            description = "Updates permissions for an existing group",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.MODIFY")
    @PutMapping("/{groupId}")
    public ResponseEntity<CommonResponse<GroupResponse>> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupCreateRequest request) {
        log.info("Admin updating group: {}", groupId);

        GroupResponse response = adminGroupUseCase.updateGroup(groupId, request);

        return ResponseEntity.ok(CommonResponse.success("Group updated successfully", response));
    }

    @Operation(
            summary = "Delete group",
            description = "Deletes a non-system group",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.MODIFY")
    @DeleteMapping("/{groupId}")
    public ResponseEntity<CommonResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        log.info("Admin deleting group: {}", groupId);

        adminGroupUseCase.deleteGroup(groupId);

        return ResponseEntity.ok(CommonResponse.success("Group deleted successfully", null));
    }

    @Operation(
            summary = "Toggle group active status",
            description = "Activates or deactivates a group",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @CivifyAuthorization(permission = "GROUP.MODIFY")
    @PatchMapping("/{groupId}/toggle-active")
    public ResponseEntity<CommonResponse<GroupResponse>> toggleGroupActive(@PathVariable Long groupId) {
        log.info("Admin toggling active status for group: {}", groupId);

        GroupResponse response = adminGroupUseCase.toggleGroupActive(groupId);

        return ResponseEntity.ok(CommonResponse.success(
                response.getIsActive() ? "Group activated" : "Group deactivated",
                response));
    }
}
