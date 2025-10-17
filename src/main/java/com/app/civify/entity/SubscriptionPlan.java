package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "subscription_plans")
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String details;

    // Feature Limits
    private Integer cvLimit; // null = unlimited
    private Integer apiLimit; // null = unlimited
    
    @Column(nullable = false)
    private Boolean watermark = true; // true = has watermark, false = no watermark
    
    @Column(nullable = false)
    private Boolean dashboard = false; // true = licensed user with dashboard access

    // Pricing
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;

    @Column(nullable = false)
    private Boolean active = true;

    // Helper method to check if this plan grants licensed status
    public boolean grantsLicensedStatus() {
        return Boolean.TRUE.equals(dashboard);
    }
}
