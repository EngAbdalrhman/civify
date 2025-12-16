package com.app.civify.service.auth;

import com.app.civify.entity.Group;
import com.app.civify.entity.Permission;
import com.app.civify.entity.User;
import com.app.civify.repository.GroupRepository;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final SubscriptionService subscriptionService;
    private final GroupRepository groupRepository;

    private static final String FREE_PLAN_GROUP_NAME = "Free User"; // Make sure this matches Init

    /**
     * Calculates the effective permissions for a user at runtime.
     * Logic:
     * 1. If System Group (Admin/Support) -> Return Group Permissions (Always active).
     * 2. If Subscription Group -> Check if Subscription is Active.
     *    - Active -> Return Group Permissions.
     *    - Expired/Inactive -> Return Default Free Group Permissions (Fallback).
     */
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissionNames(User user) {
        if (user == null || user.getGroup() == null) {
            return Collections.emptySet();
        }

        Group group = user.getGroup();

        // 1. System Groups (Admins) always have their permissions
        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            return extractPermissionNames(group.getPermissions());
        }

        // 2. Subscription Groups
        boolean isSubscriptionActive = subscriptionService.getActiveSubscription(user.getId()).isPresent();

        if (isSubscriptionActive) {
            return extractPermissionNames(group.getPermissions());
        } else {
            // 3. Fallback to Free Group Permissions
            log.debug("User {} subscription expired/inactive. Falling back to Free Group permissions.", user.getEmail());
            return getFreeGroupPermissions();
        }
    }

    private Set<String> extractPermissionNames(List<Permission> permissions) {
        if (permissions == null) return Collections.emptySet();
        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> getFreeGroupPermissions() {
        return groupRepository.findByName(FREE_PLAN_GROUP_NAME)
                .map(Group::getPermissions)
                .map(this::extractPermissionNames)
                .orElseGet(() -> {
                    log.error("Default Free Group not found! Returning empty permissions.");
                    return Collections.emptySet();
                });
    }

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(User user, String permissionName) {
        Set<String> effectivePermissions = getEffectivePermissionNames(user);
        return effectivePermissions.contains(permissionName);
    }
}
