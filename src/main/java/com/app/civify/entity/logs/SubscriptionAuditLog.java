package com.app.civify.entity.logs;

import com.app.civify.entity.enums.SubscriptionAuditType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks subscription lifecycle events from schedulers and user actions.
 */
@Entity
@Table(name = "subscription_audit_logs", indexes = {
    @Index(name = "idx_sub_audit_user", columnList = "user_id"),
    @Index(name = "idx_sub_audit_subscription", columnList = "subscription_id"),
    @Index(name = "idx_sub_audit_type", columnList = "audit_type"),
    @Index(name = "idx_sub_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 30)
    private SubscriptionAuditType auditType;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
