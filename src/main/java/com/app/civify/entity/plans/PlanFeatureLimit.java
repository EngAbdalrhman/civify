package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Join entity representing the relationship between SubscriptionPlan and PlanFeature
 * Stores the limit value for each feature in a plan
 */
@Entity
@Data
@Table(name = "plan_feature_limits", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "feature_id"}))
@EqualsAndHashCode(callSuper = true)
public class PlanFeatureLimit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "feature_id", nullable = false)
    private PlanFeature feature;

    /**
     * Limit value for features with limits (e.g., 100 for CV generation)
     * null = unlimited
     * For boolean features, use 1 for enabled, 0 for disabled
     */
    private Integer limitValue;

    /**
     * Whether this feature is enabled for the plan
     * For boolean features (hasLimit = false)
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * Additional metadata or configuration for the feature (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Display order for UI
     */
    private Integer displayOrder = 0;

    /**
     * Check if feature is unlimited
     */
    public boolean isUnlimited() {
        return limitValue == null && enabled;
    }

    /**
     * Get effective limit (null for unlimited, 0 for disabled)
     */
    public Integer getEffectiveLimit() {
        if (!enabled) {
            return 0;
        }
        return limitValue; // null means unlimited
    }
}

