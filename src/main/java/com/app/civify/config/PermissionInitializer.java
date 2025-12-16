package com.app.civify.config;

import com.app.civify.entity.Group;
import com.app.civify.entity.Permission;
import com.app.civify.entity.SubscriptionPlan;
import com.app.civify.repository.GroupRepository;
import com.app.civify.repository.PermissionRepository;
import com.app.civify.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final GroupRepository groupRepository;
    private final SubscriptionPlanRepository planRepository;

    // --- PERMISSION CONSTANTS ---
    // User / CV
    public static final String CV_CREATE = "CV.CREATE";
    public static final String CV_EXPORT_PDF = "CV.EXPORT.PDF";
    public static final String CV_EXPORT_DOCX = "CV.EXPORT.DOCX";
    public static final String CV_NO_WATERMARK = "CV.NO_WATERMARK";
    
    // API
    public static final String API_ACCESS = "API.ACCESS";

    // ADMIN - USERS
    public static final String USER_VIEW = "USER.VIEW";
    public static final String USER_MODIFY = "USER.MODIFY"; // Block, Delete, Edit

    // ADMIN - PLANS
    public static final String PLAN_VIEW = "PLAN.VIEW";
    public static final String PLAN_MODIFY = "PLAN.MODIFY";

    // ADMIN - SUBSCRIPTIONS
    public static final String SUB_VIEW = "SUB.VIEW";
    public static final String SUB_MODIFY = "SUB.MODIFY";

    // ADMIN - PROMOS
    public static final String PROMO_VIEW = "PROMO.VIEW";
    public static final String PROMO_MODIFY = "PROMO.MODIFY";

    // ADMIN - GROUPS
    public static final String GROUP_VIEW = "GROUP.VIEW";
    public static final String GROUP_MODIFY = "GROUP.MODIFY";
    
    // ADMIN - SYSTEM
    public static final String SYSTEM_LOGS_VIEW = "SYSTEM.LOGS.VIEW";
    public static final String NOTIF_SEND = "NOTIF.SEND";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing Permissions and Groups...");

        // 1. Init Permissions
        initPermission(CV_CREATE, "Create and edit Resumes", "CV");
        initPermission(CV_EXPORT_PDF, "Export Resume as PDF", "CV");
        initPermission(CV_EXPORT_DOCX, "Export Resume as DOCX", "CV");
        initPermission(CV_NO_WATERMARK, "Remove Watermark from exports", "CV");
        initPermission(API_ACCESS, "Access Developer API", "API");

        initPermission(USER_VIEW, "View Users List and Details", "ADMIN");
        initPermission(USER_MODIFY, "Modify Users (Block/Delete)", "ADMIN");
        initPermission(PLAN_VIEW, "View Subscription Plans", "ADMIN");
        initPermission(PLAN_MODIFY, "Create/Edit Subscription Plans", "ADMIN");
        initPermission(SUB_VIEW, "View User Subscriptions", "ADMIN");
        initPermission(SUB_MODIFY, "Modify User Subscriptions", "ADMIN");
        initPermission(PROMO_VIEW, "View Promo Codes", "ADMIN");
        initPermission(PROMO_MODIFY, "Manage Promo Codes", "ADMIN");
        initPermission(GROUP_VIEW, "View User Groups", "ADMIN");
        initPermission(GROUP_MODIFY, "Manage User Groups", "ADMIN");
        initPermission(SYSTEM_LOGS_VIEW, "View System Logs", "ADMIN");
        initPermission(NOTIF_SEND, "Send Push Notifications", "ADMIN");

        // 2. Init Groups
        Group adminGroup = initGroup("Super Admin", true, false);
        Group supportGroup = initGroup("Support", true, false);
        
        Group freeUserGroup = initGroup("Free User", false, true); // Default group
        Group proUserGroup = initGroup("Pro User", false, false);

        // 3. Assign Permissions to Groups
        
        // Super Admin -> ALL Permissions
        List<Permission> allPerms = permissionRepository.findAll();
        updateGroupPermissions(adminGroup, allPerms);

        // Support -> VIEW only (and basic user perms)
        List<Permission> supportPerms = permissionRepository.findAll().stream()
                .filter(p -> p.getName().endsWith(".VIEW") || p.getName().startsWith("CV."))
                .collect(Collectors.toList());
        updateGroupPermissions(supportGroup, supportPerms);

        // Free User -> Basic CV
        List<Permission> freePerms = Arrays.asList(
                getPerm(CV_CREATE),
                getPerm(CV_EXPORT_PDF)
        );
        updateGroupPermissions(freeUserGroup, freePerms);

        // Pro User -> All CV + API + No Watermark
        List<Permission> proPerms = Arrays.asList(
                getPerm(CV_CREATE),
                getPerm(CV_EXPORT_PDF),
                getPerm(CV_EXPORT_DOCX),
                getPerm(CV_NO_WATERMARK),
                getPerm(API_ACCESS)
        );
        updateGroupPermissions(proUserGroup, proPerms);

        // 4. Link Plans to Groups
        linkPlanToGroup("free", freeUserGroup);
        linkPlanToGroup("monthly", proUserGroup); // Assuming 'monthly' is the standard pro plan slug
        linkPlanToGroup("yearly", proUserGroup);
        
        log.info("Permissions and Groups Initialized Successfully.");
    }

    private void initPermission(String name, String desc, String category) {
        if (!permissionRepository.existsByName(name)) {
            Permission p = Permission.builder()
                    .name(name)
                    .description(desc)
                    .category(category)
                    .build();
            permissionRepository.save(p);
        }
    }

    private Group initGroup(String name, boolean isSystem, boolean isDefault) {
        Optional<Group> existing = groupRepository.findByName(name);
        if (existing.isPresent()) {
            Group g = existing.get();
            boolean changed = false;
            
            if (g.getIsSystemGroup() != isSystem) {
                g.setIsSystemGroup(isSystem);
                changed = true;
            }
            if (g.getIsDefault() != isDefault) {
                g.setIsDefault(isDefault);
                changed = true;
            }
            // If we are setting this to default, ensure others are not default? 
            // Better done in a separate sweep or constraint, but for now this update ensures the target state.
            
            if (changed) {
                return groupRepository.save(g);
            }
            return g;
        } else {
            return groupRepository.save(Group.builder()
                    .name(name)
                    .isSystemGroup(isSystem)
                    .isDefault(isDefault)
                    .build());
        }
    }

    private void updateGroupPermissions(Group group, List<Permission> perms) {
        group.setPermissions(perms);
        groupRepository.save(group);
    }
    
    private Permission getPerm(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + name));
    }

    private void linkPlanToGroup(String planName, Group group) {
        // Try exact match or ignore case
        Optional<SubscriptionPlan> planOpt = planRepository.findByName(planName);
        if (planOpt.isEmpty()) {
            // Try to find by partial match or standard names if DB has different names
            // For now, log warning
            log.warn("Plan '{}' not found. Skipping Group linkage.", planName);
            return;
        }
        SubscriptionPlan plan = planOpt.get();
        if (plan.getLinkedGroup() == null || !plan.getLinkedGroup().getId().equals(group.getId())) {
            plan.setLinkedGroup(group);
            planRepository.save(plan);
            log.info("Linked Plan '{}' to Group '{}'", planName, group.getName());
        }
    }
}
