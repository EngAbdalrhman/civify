package com.app.civify.entity.logs;

import com.app.civify.entity.enums.BillingAuditType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tracks billing and payment events including webhook processing.
 */
@Entity
@Table(name = "billing_audit_logs", indexes = {
    @Index(name = "idx_billing_audit_user", columnList = "user_id"),
    @Index(name = "idx_billing_audit_invoice", columnList = "invoice_id"),
    @Index(name = "idx_billing_audit_txn", columnList = "transaction_id"),
    @Index(name = "idx_billing_audit_type", columnList = "audit_type"),
    @Index(name = "idx_billing_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, length = 30)
    private BillingAuditType auditType;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(length = 50)
    private String gateway;

    @Column(name = "gateway_invoice_id", length = 100)
    private String gatewayInvoiceId;

    @Column(name = "gateway_status", length = 50)
    private String gatewayStatus;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webhook_payload", columnDefinition = "jsonb")
    private String webhookPayload;

    @Column(length = 255)
    private String result;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
