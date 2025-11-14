package com.app.civify.entity;

import com.app.civify.entity.enums.BillingCycle;
import com.app.civify.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents discounts that can be applied to subscription plans
 * Supports percentage and fixed amount discounts for different billing cycles
 */
@Entity
@Data
@Table(name = "plan_discounts")
@EqualsAndHashCode(callSuper = true)
public class PlanDiscount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    /**
     * Name of the discount (e.g., "Annual Discount", "Black Friday Sale")
     */
    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    /**
     * Discount value (percentage or fixed amount depending on type)
     * For PERCENTAGE: 0-100 (e.g., 20 for 20% off)
     * For FIXED_AMOUNT: actual amount (e.g., 10.00 for $10 off)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Billing cycle this discount applies to (null = all cycles)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle")
    private BillingCycle billingCycle;

    /**
     * Start date of the discount (null = no start restriction)
     */
    private LocalDateTime startDate;

    /**
     * End date of the discount (null = no end restriction)
     */
    private LocalDateTime endDate;

    /**
     * Whether this discount is currently active
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Maximum number of times this discount can be used (null = unlimited)
     */
    private Integer maxUses;

    /**
     * Current number of times this discount has been used
     */
    @Column(nullable = false)
    private Integer currentUses = 0;

    /**
     * Priority/order for applying multiple discounts (higher = applied first)
     */
    private Integer priority = 0;

    /**
     * Check if discount is currently valid
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Check date range
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }

        // Check usage limit
        if (maxUses != null && currentUses >= maxUses) {
            return false;
        }

        return true;
    }

    /**
     * Calculate discount amount for a given price
     */
    public BigDecimal calculateDiscount(BigDecimal price) {
        if (!isValid() || price == null) {
            return BigDecimal.ZERO;
        }

        if (discountType == DiscountType.PERCENTAGE) {
            return price.multiply(discountValue).divide(
                BigDecimal.valueOf(100),
                2,
                java.math.RoundingMode.HALF_UP
            );
        } else {
            // Fixed amount - but don't exceed the price
            return discountValue.min(price);
        }
    }

    /**
     * Increment usage counter
     */
    public void incrementUses() {
        if (currentUses == null) {
            currentUses = 0;
        }
        currentUses++;
    }
}

