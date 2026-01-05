package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.security.Group;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;

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
    /**
     * Optional monthly token allowance for AI/LLM usage (in tokens).
     * If set, requests will be billed against this budget instead of raw call counts.
     */
    private Integer tokenLimit;

    @Column(nullable = false)
    private Boolean watermark = true; // true = has watermark, false = no watermark

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "linked_group_id")
    private Group linkedGroup;

    // Pricing
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;

    @Column(nullable = false)
    private Boolean active = true;

    // ==================== BILLING RESTRICTIONS ====================
    /**
     * Whether annual billing is allowed for this plan.
     * If false, only monthly billing is available.
     */
    @Column
    @ColumnDefault("true")
    private Boolean allowAnnualBilling = true;

    /**
     * Whether users can renew this plan.
     * If false, users must switch to a different plan after expiry.
     */
    @Column
    @ColumnDefault("true")
    private Boolean renewalAllowed = true;

    /**
     * Maximum billing cycles allowed for this plan (e.g., 1-2 for beta plans).
     * Null means unlimited cycles.
     */
    @Column
    private Integer maxBillingCycles;

    // ==================== DISPLAY SETTINGS ====================
    /**
     * Whether this plan is featured/highlighted in the UI.
     */
    @Column
    @ColumnDefault("false")
    private Boolean isFeatured = false;

    /**
     * Sort order for displaying plans (lower numbers first).
     */
    @Column
    @ColumnDefault("0")
    private Integer sortOrder = 0;

    // ==================== NEW RELATIONSHIPS ====================
    /**
     * Features included in this plan with their limits
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PlanFeatureLimit> featureLimits = new ArrayList<>();

    /**
     * Pricing options for different billing cycles
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PlanPricing> pricingOptions = new ArrayList<>();

    /**
     * Discounts available for this plan
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PlanDiscount> discounts = new ArrayList<>();

    /**
     * Integrations available in this plan
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PlanIntegration> integrations = new ArrayList<>();

    /**
     * Currency-specific price overrides
     * If an override exists for a currency, use it instead of converting from USD
     */
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlanCurrencyPrice> currencyPrices = new ArrayList<>();

    // ==================== HELPER METHODS ====================

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
