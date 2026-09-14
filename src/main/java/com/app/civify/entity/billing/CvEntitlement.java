package com.app.civify.entity.billing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "cv_entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resume_id", length = 36)
    private String resumeId;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "entitlement_type", nullable = false, length = 50)
    @Builder.Default
    private String entitlementType = "SINGLE_CV";

    @Column(name = "credits_remaining", nullable = false)
    @Builder.Default
    private Integer creditsRemaining = 1;

    @Column(name = "watermark_free", nullable = false)
    @Builder.Default
    private Boolean watermarkFree = true;

    @Column(name = "allow_doc_export", nullable = false)
    @Builder.Default
    private Boolean allowDocExport = true;

    @Column(name = "unlimited_edits_until", nullable = false)
    private Instant unlimitedEditsUntil;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isValid() {
        return "ACTIVE".equalsIgnoreCase(status)
                && unlimitedEditsUntil != null
                && unlimitedEditsUntil.isAfter(Instant.now());
    }
}
