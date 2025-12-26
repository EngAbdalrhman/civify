package com.app.civify.controller.v1.security;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.group.GroupCreateRequest;
import com.app.civify.dto.group.GroupResponse;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.security.Group;
import com.app.civify.entity.security.Permission;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.security.PermissionRepository;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.audit.AuditService;
import com.app.civify.util.CommonResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for managing user groups and permissions
 */
@Slf4j
@ApiV1Controller
@RequestMapping("/admin/groups")
@RequiredArgsConstructor
@Tag(name = "Admin Group Management", description = "Admin endpoints for managing groups and permissions")
public class AdminGroupController {

    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;
    private final CommonService commonService;
    private final AuditService auditService;

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

        Page<Group> groupPage = commonService.filterEntities(groupRepository, inputData);
        
        List<GroupResponse> responses = groupPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PagedResponse<GroupResponse> pagedResponse = new PagedResponse<>();
        pagedResponse.setContent(responses);
        pagedResponse.setPage(groupPage.getNumber());
        pagedResponse.setSize(groupPage.getSize());
        pagedResponse.setTotalElements(groupPage.getTotalElements());
        pagedResponse.setTotalPages(groupPage.getTotalPages());
        pagedResponse.setFirst(groupPage.isFirst());
        pagedResponse.setLast(groupPage.isLast());
        pagedResponse.setHasNext(groupPage.hasNext());
        pagedResponse.setHasPrevious(groupPage.hasPrevious());

        return ResponseEntity.ok(CommonResponse.success("Groups filtered successfully", pagedResponse));
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

        List<GroupResponse> responses = groupRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

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

        List<GroupResponse.PermissionDto> permissions = permissionRepository.findAll().stream()
                .map(p -> GroupResponse.PermissionDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .category(p.getCategory())
                        .build())
                .collect(Collectors.toList());

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

        Group group = groupRepository.findByIdWithPermissions(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        return ResponseEntity.ok(CommonResponse.success("Group retrieved successfully", toResponse(group)));
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

        // Check if name already exists
        if (groupRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Group with name already exists: " + request.getName());
        }

        // Build group
        Group group = Group.builder()
                .name(request.getName())
                .isSystemGroup(false)
                .isDefault(false)
                .build();

        // Assign permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            group.setPermissions(permissions);
        }

        Group saved = groupRepository.save(group);
        log.info("Created group: {} with {} permissions", saved.getName(), 
                saved.getPermissions() != null ? saved.getPermissions().size() : 0);

        logAudit(saved.getId(), AuditAction.CREATE, null, groupToMap(saved),
                "Created group: " + saved.getName());

        return ResponseEntity.ok(CommonResponse.success("Group created successfully", toResponse(saved)));
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

        Group group = groupRepository.findByIdWithPermissions(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // Prevent modifying system groups
        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            throw new BadRequestException("Cannot modify system group: " + group.getName());
        }

        // Update name if provided
        if (request.getName() != null && !request.getName().equals(group.getName())) {
            if (groupRepository.findByName(request.getName()).isPresent()) {
                throw new BadRequestException("Group with name already exists: " + request.getName());
            }
            group.setName(request.getName());
        }

        // Update permissions
        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            group.setPermissions(permissions);
        }

        Group saved = groupRepository.save(group);
        log.info("Updated group: {} with {} permissions", saved.getName(), 
                saved.getPermissions() != null ? saved.getPermissions().size() : 0);

        logAudit(saved.getId(), AuditAction.UPDATE, null, groupToMap(saved),
                "Updated group: " + saved.getName());

        return ResponseEntity.ok(CommonResponse.success("Group updated successfully", toResponse(saved)));
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

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // Prevent deleting system groups
        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            throw new BadRequestException("Cannot delete system group: " + group.getName());
        }

        // Prevent deleting default group
        if (Boolean.TRUE.equals(group.getIsDefault())) {
            throw new BadRequestException("Cannot delete default group: " + group.getName());
        }

        String groupName = group.getName();
        groupRepository.delete(group);
        log.info("Deleted group: {}", groupName);

        logAudit(groupId, AuditAction.DELETE, groupToMap(group), null,
                "Deleted group: " + groupName);

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

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // Prevent modifying system groups
        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            throw new BadRequestException("Cannot modify system group: " + group.getName());
        }

        // Toggle active status
        group.setIsActive(!Boolean.TRUE.equals(group.getIsActive()));
        Group saved = groupRepository.save(group);
        
        log.info("Toggled active status for group: {} to {}", saved.getName(), saved.getIsActive());

        String action = saved.getIsActive() ? "Activated" : "Deactivated";
        logAudit(saved.getId(), AuditAction.UPDATE, null, null,
                action + " group: " + saved.getName());

        return ResponseEntity.ok(CommonResponse.success(
                saved.getIsActive() ? "Group activated" : "Group deactivated", 
                toResponse(saved)));
    }

    private GroupResponse toResponse(Group group) {
        List<GroupResponse.PermissionDto> permissionDtos = null;
        if (group.getPermissions() != null) {
            permissionDtos = group.getPermissions().stream()
                    .map(p -> GroupResponse.PermissionDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .description(p.getDescription())
                            .category(p.getCategory())
                            .build())
                    .collect(Collectors.toList());
        }

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .isSystemGroup(group.getIsSystemGroup())
                .isDefault(group.getIsDefault())
                .isActive(group.getIsActive())
                .permissions(permissionDtos)
                .build();
    }

    private void logAudit(Long entityId, AuditAction action, Map<String, Object> oldVal,
                          Map<String, Object> newVal, String comment) {
        try {
            auditService.logAudit("Group", entityId.toString(), action, oldVal, newVal, comment);
        } catch (Exception e) {
            log.warn("Failed to log audit for Group action: {}", action, e);
        }
    }

    private Map<String, Object> groupToMap(Group group) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", group.getName());
        map.put("isSystemGroup", group.getIsSystemGroup());
        map.put("isDefault", group.getIsDefault());
        map.put("isActive", group.getIsActive());
        map.put("permissionCount", group.getPermissions() != null ? group.getPermissions().size() : 0);
        return map;
    }
}

