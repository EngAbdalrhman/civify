package com.app.civify.entity;

import com.app.civify.entity.enums.BillingCycle;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Represents pricing for a subscription plan with different billing cycles
 * Allows flexible pricing models (monthly, quarterly, annual, etc.)
 */
@Entity
@Data
@Table(name = "plan_pricing",
       uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "billing_cycle"}))
@EqualsAndHashCode(callSuper = true)
public class PlanPricing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    /**
     * Price for this billing cycle
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Original price before discount (for display purposes)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Discount percentage applied (0-100)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Currency code (e.g., USD, EUR, GBP)
     */
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    /**
     * Whether this pricing option is active
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Display order for UI
     */
    private Integer displayOrder = 0;

    /**
     * Whether this is the recommended/popular option
     */
    @Column(nullable = false)
    private Boolean recommended = false;

    /**
     * Calculate savings compared to monthly price
     */
    public BigDecimal calculateSavings(BigDecimal monthlyPrice) {
        if (monthlyPrice == null || billingCycle.isLifetime()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalMonthlyEquivalent = monthlyPrice.multiply(
            BigDecimal.valueOf(billingCycle.getMonths())
        );
        
        return totalMonthlyEquivalent.subtract(price);
    }

    /**
     * Calculate effective monthly price
     */
    public BigDecimal getEffectiveMonthlyPrice() {
        if (billingCycle.isLifetime() || billingCycle.getMonths() <= 0) {
            return price;
        }
        
        return price.divide(
            BigDecimal.valueOf(billingCycle.getMonths()),
            2,
            java.math.RoundingMode.HALF_UP
        );
    }
}

