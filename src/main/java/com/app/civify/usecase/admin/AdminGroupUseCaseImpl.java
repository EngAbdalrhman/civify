package com.app.civify.usecase.admin;

import com.app.civify.dto.group.GroupCreateRequest;
import com.app.civify.dto.group.GroupResponse;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.entity.security.Group;
import com.app.civify.entity.security.Permission;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.security.PermissionRepository;
import com.app.civify.service.audit.AuditService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of AdminGroupUseCase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminGroupUseCaseImpl implements AdminGroupUseCase {

    private final GroupRepository groupRepository;
    private final PermissionRepository permissionRepository;
    private final CommonService commonService;
    private final AuditService auditService;

    @Override
    public PagedResponse<GroupResponse> filterGroups(InputDataModel inputData) {
        log.info("Filtering groups with criteria: {}", inputData);

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

        return pagedResponse;
    }

    @Override
    public List<GroupResponse> getAllGroups() {
        log.info("Fetching all groups");
        return groupRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupResponse.PermissionDto> getAllPermissions() {
        log.info("Fetching all permissions");
        return permissionRepository.findAll().stream()
                .map(p -> GroupResponse.PermissionDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .category(p.getCategory())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public GroupResponse getGroupById(Long groupId) {
        log.info("Fetching group with ID: {}", groupId);
        Group group = groupRepository.findByIdWithPermissions(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        return toResponse(group);
    }

    @Override
    @Transactional
    public GroupResponse createGroup(GroupCreateRequest request) {
        log.info("Creating group: {}", request.getName());

        if (groupRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Group with name already exists: " + request.getName());
        }

        Group group = Group.builder()
                .name(request.getName())
                .isSystemGroup(false)
                .isDefault(false)
                .build();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            group.setPermissions(permissions);
        }

        Group saved = groupRepository.save(group);
        log.info("Created group: {} with {} permissions", saved.getName(),
                saved.getPermissions() != null ? saved.getPermissions().size() : 0);

        logAudit(saved.getId(), AuditAction.CREATE, null, groupToMap(saved),
                "Created group: " + saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long groupId, GroupCreateRequest request) {
        log.info("Updating group: {}", groupId);

        Group group = groupRepository.findByIdWithPermissions(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            throw new BadRequestException("Cannot modify system group: " + group.getName());
        }

        if (request.getName() != null && !request.getName().equals(group.getName())) {
            if (groupRepository.findByName(request.getName()).isPresent()) {
                throw new BadRequestException("Group with name already exists: " + request.getName());
            }
            group.setName(request.getName());
        }

        if (request.getPermissionIds() != null) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            group.setPermissions(permissions);
        }

        Group saved = groupRepository.save(group);
        log.info("Updated group: {} with {} permissions", saved.getName(),
                saved.getPermissions() != null ? saved.getPermissions().size() : 0);

        logAudit(saved.getId(), AuditAction.UPDATE, null, groupToMap(saved),
                "Updated group: " + saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        log.info("Deleting group: {}", groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            throw new BadRequestException("Cannot delete system group: " + group.getName());
        }

        if (Boolean.TRUE.equals(group.getIsDefault())) {
            throw new BadRequestException("Cannot delete default group: " + group.getName());
        }

        String groupName = group.getName();
        groupRepository.delete(group);
        log.info("Deleted group: {}", groupName);

        logAudit(groupId, AuditAction.DELETE, groupToMap(group), null,
                "Deleted group: " + groupName);
    }

    @Override
    @Transactional
    public GroupResponse toggleGroupActive(Long groupId) {
        log.info("Toggling active status for group: {}", groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            throw new BadRequestException("Cannot modify system group: " + group.getName());
        }

        group.setIsActive(!Boolean.TRUE.equals(group.getIsActive()));
        Group saved = groupRepository.save(group);

        log.info("Toggled active status for group: {} to {}", saved.getName(), saved.getIsActive());

        String action = saved.getIsActive() ? "Activated" : "Deactivated";
        logAudit(saved.getId(), AuditAction.UPDATE, null, null,
                action + " group: " + saved.getName());

        return toResponse(saved);
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
