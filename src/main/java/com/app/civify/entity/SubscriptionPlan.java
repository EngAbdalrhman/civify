package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    // ==================== NEW RELATIONSHIPS ====================
    /**
     * Features included in this plan with their limits
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanFeatureLimit> featureLimits = new ArrayList<>();

    /**
     * Pricing options for different billing cycles
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanPricing> pricingOptions = new ArrayList<>();

    /**
     * Discounts available for this plan
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanDiscount> discounts = new ArrayList<>();

    /**
     * Integrations available in this plan
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanIntegration> integrations = new ArrayList<>();

    // ==================== HELPER METHODS ====================
    /**
     * Check if this plan grants licensed status
     *
     * @deprecated Use featureLimits to check for DASHBOARD_ACCESS feature
     */
    @Deprecated
    public boolean grantsLicensedStatus() {
        return Boolean.TRUE.equals(dashboard);
    }

    /**
     * Add a feature limit to this plan
     */
    public void addFeatureLimit(PlanFeatureLimit featureLimit) {
        featureLimits.add(featureLimit);
        featureLimit.setPlan(this);
    }

    /**
     * Remove a feature limit from this plan
     */
    public void removeFeatureLimit(PlanFeatureLimit featureLimit) {
        featureLimits.remove(featureLimit);
        featureLimit.setPlan(null);
    }

    /**
     * Add a pricing option to this plan
     */
    public void addPricingOption(PlanPricing pricing) {
        pricingOptions.add(pricing);
        pricing.setPlan(this);
    }

    /**
     * Remove a pricing option from this plan
     */
    public void removePricingOption(PlanPricing pricing) {
        pricingOptions.remove(pricing);
        pricing.setPlan(null);
    }

    /**
     * Add a discount to this plan
     */
    public void addDiscount(PlanDiscount discount) {
        discounts.add(discount);
        discount.setPlan(this);
    }

    /**
     * Remove a discount from this plan
     */
    public void removeDiscount(PlanDiscount discount) {
        discounts.remove(discount);
        discount.setPlan(null);
    }

    /**
     * Add an integration to this plan
     */
    public void addIntegration(PlanIntegration integration) {
        integrations.add(integration);
        integration.setPlan(this);
    }

    /**
     * Remove an integration from this plan
     */
    public void removeIntegration(PlanIntegration integration) {
        integrations.remove(integration);
        integration.setPlan(null);
    }
}
