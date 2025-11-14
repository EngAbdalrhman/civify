package com.app.civify.entity;

import com.app.civify.entity.enums.FeatureType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a feature that can be included in subscription plans
 * Features can have limits (e.g., 100 CV generations) or be boolean (e.g., watermark removal)
 */
@Entity
@Data
@Table(name = "plan_features")
@EqualsAndHashCode(callSuper = true)
public class PlanFeature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private FeatureType featureType;

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * Whether this feature has a numeric limit (e.g., CV count, API calls)
     * If false, it's a boolean feature (enabled/disabled)
     */
    @Column(nullable = false)
    private Boolean hasLimit = false;

    /**
     * Unit of measurement for limited features (e.g., "CVs", "API calls", "GB")
     */
    private String unit;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Display order for UI
     */
    private Integer displayOrder = 0;
}

