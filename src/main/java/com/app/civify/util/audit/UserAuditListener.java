package com.app.civify.util.audit;

import com.app.civify.entity.User;
import com.app.civify.entity.enums.AuditAction;
import com.app.civify.service.audit.AuditService;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JPA Entity Listener for User entity
 * Automatically logs audit entries for User entity changes
 */
@Slf4j
@Component
public class UserAuditListener {

    private static ApplicationContext context;
    private static AuditService auditService;

    /**
     * Set application context (called by Spring)
     */
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /**
     * Get AuditService bean lazily
     */
    private AuditService getAuditService() {
        if (auditService == null && context != null) {
            auditService = context.getBean(AuditService.class);
        }
        return auditService;
    }

    /**
     * Called after entity is persisted (created)
     */
    @PostPersist
    public void onPostPersist(User user) {
        try {
            AuditService service = getAuditService();
            if (service != null) {
                Map<String, Object> newValue = EntityAuditUtil.entityToMap(user);
                service.logAudit(
                    "User",
                    EntityAuditUtil.getEntityId(user),
                    AuditAction.CREATE,
                    null,
                    newValue,
                    "User created"
                );
                log.debug("Audit log created for new user: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to create audit log for user creation: {}", e.getMessage(), e);
        }
    }

    /**
     * Called after entity is updated
     */
    @PostUpdate
    public void onPostUpdate(User user) {
        try {
            AuditService service = getAuditService();
            if (service != null) {
                // Note: We don't have access to old values in @PostUpdate
                // For full old/new value tracking, consider using Hibernate Envers or custom solution
                Map<String, Object> newValue = EntityAuditUtil.entityToMap(user);
                service.logAudit(
                    "User",
                    EntityAuditUtil.getEntityId(user),
                    AuditAction.UPDATE,
                    null,  // Old value not available in @PostUpdate
                    newValue,
                    "User updated"
                );
                log.debug("Audit log created for user update: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to create audit log for user update: {}", e.getMessage(), e);
        }
    }

    /**
     * Called after entity is removed (deleted)
     */
    @PostRemove
    public void onPostRemove(User user) {
        try {
            AuditService service = getAuditService();
            if (service != null) {
                Map<String, Object> oldValue = EntityAuditUtil.entityToMap(user);
                service.logAudit(
                    "User",
                    EntityAuditUtil.getEntityId(user),
                    AuditAction.DELETE,
                    oldValue,
                    null,
                    "User deleted"
                );
                log.debug("Audit log created for user deletion: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to create audit log for user deletion: {}", e.getMessage(), e);
        }
    }
}

