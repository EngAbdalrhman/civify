package com.app.civify.service.auth;

import com.app.civify.entity.security.Group;
import com.app.civify.entity.security.Permission;
import com.app.civify.entity.User;
import com.app.civify.repository.security.GroupRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final UserRepository userRepository;

    private static final String FREE_PLAN_GROUP_NAME = "Free User"; // Make sure this matches Init

    /**
     * Calculates the effective permissions for a user at runtime.
     * Results are cached for 30 minutes to avoid repeated DB queries.
     * Logic:
     * 1. If System Group (Admin/Support) -> Return Group Permissions (Always active).
     * 2. If Subscription Group -> Check if Subscription is Active.
     *    - Active -> Return Group Permissions.
     *    - Expired/Inactive -> Return Default Free Group Permissions (Fallback).
     */
    @Cacheable(value = "user-permissions", key = "#userId")
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissionNames(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        User user = getUserById(userId);
        if (user == null || user.getGroup() == null) {
            log.warn("User {} not found or has no group assigned", userId);
            return Collections.emptySet();
        }

        Group group = user.getGroup();

        // 1. System Groups (Admins) always have their permissions
        if (Boolean.TRUE.equals(group.getIsSystemGroup())) {
            log.debug("User {} is system group '{}', returning full permissions", user.getEmail(), group.getName());
            return extractPermissionNames(group.getPermissions());
        }

        // 2. Subscription Groups
        boolean isSubscriptionActive = subscriptionService.getActiveSubscription(userId).isPresent();

        if (isSubscriptionActive) {
            log.debug("User {} has active subscription, returning group permissions", user.getEmail());
            return extractPermissionNames(group.getPermissions());
        } else {
            // 3. Fallback to Free Group Permissions
            log.debug("User {} subscription expired/inactive. Falling back to Free Group permissions.", user.getEmail());
            return getFreeGroupPermissions();
        }
    }

    /**
     * Compatibility method for existing code that passes User object
     */
    public Set<String> getEffectivePermissionNames(User user) {
        if (user == null) return Collections.emptySet();
        return getEffectivePermissionNames(user.getId());
    }

    /**
     * Invalidate permission cache for a user.
     * Call this when:
     * - User's subscription changes
     * - User's group changes
     * - Plan's linkedGroup changes
     */
    @CacheEvict(value = "user-permissions", key = "#userId")
    public void invalidatePermissionCache(Long userId) {
        log.info("✅ Invalidated permission cache for user {}", userId);
    }

    /**
     * Helper method to fetch user by ID directly from repository
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
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
