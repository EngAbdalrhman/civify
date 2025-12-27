package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.enums.IntegrationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents third-party integrations and AI models available in subscription plans
 */
@Entity
@Data
@Table(name = "plan_integrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "integration_type", "integration_name"}))
@EqualsAndHashCode(callSuper = true)
public class PlanIntegration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false)
    private IntegrationType integrationType;

    /**
     * Name of the integration (e.g., "GPT-4", "Google Drive", "LinkedIn")
     */
    @Column(name = "integration_name", nullable = false)
    private String integrationName;

    private String description;

    /**
     * Whether this integration is enabled for the plan
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * Usage limit for this integration (null = unlimited)
     * For AI models: number of API calls
     * For storage: GB limit
     */
    private Integer usageLimit;

    /**
     * Unit of measurement for usage limit (e.g., "calls", "GB", "requests")
     */
    private String usageUnit;

    /**
     * Configuration or metadata for the integration (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String configuration;

    /**
     * Display order for UI
     */
    private Integer displayOrder = 0;

    /**
     * Icon or logo URL for the integration
     */
    private String iconUrl;

    /**
     * Documentation or help URL
     */
    private String documentationUrl;

    /**
     * Check if integration has unlimited usage
     */
    public boolean isUnlimited() {
        return usageLimit == null && enabled;
    }
}

