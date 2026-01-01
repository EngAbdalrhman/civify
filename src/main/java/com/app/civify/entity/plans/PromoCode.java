package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.enums.DiscountType;
import com.app.civify.entity.enums.PromoApplicabilityType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class PromoCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // PERCENTAGE or FIXED_AMOUNT

    private BigDecimal discountValue; // e.g., 20.00 for $20 off or 0.20 for 20%

    private BigDecimal minimumPurchase; // Minimum purchase amount to use promo

    private Integer usageLimit; // Maximum number of uses

    private Integer usedCount = 0; // Current usage count

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean active = true;

    /**
     * What this promo can be applied to: PLAN, TOKEN, or ALL
     */
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ALL'")
    @Column(nullable = false)
    private PromoApplicabilityType applicabilityType = PromoApplicabilityType.ALL;

    /**
     * Maximum discount amount for percentage discounts (cap).
     * E.g., 20% off up to max 50 EGP. Null means no cap.
     */
    private BigDecimal maxDiscountAmount;

    /**
     * Specific plans this promo applies to. Empty/null means all plans.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "promo_code_applicable_plans",
        joinColumns = @JoinColumn(name = "promo_code_id"),
        inverseJoinColumns = @JoinColumn(name = "plan_id")
    )
    private List<SubscriptionPlan> applicablePlans = new ArrayList<>();

    public boolean isValid() {
        return active
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()))
                && (usageLimit == null || usedCount < usageLimit);
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (!isValid() || amount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            // discountValue is percentage (e.g., 20 for 20%)
            discount = amount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            // Apply cap if set
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            // Fixed amount - don't exceed purchase amount
            discount = discountValue.min(amount);
        }
        return discount;
    }

    /**
     * Check if this promo is applicable to a specific plan.
     */
    public boolean isApplicableToPlan(Long planId) {
        if (applicabilityType == PromoApplicabilityType.TOKEN) {
            return false; // Token-only promo
        }
        if (applicablePlans == null || applicablePlans.isEmpty()) {
            return true; // Applies to all plans
        }
        return applicablePlans.stream().anyMatch(p -> p.getId().equals(planId));
    }

    /**
     * Check if this promo is applicable to token purchases.
     */
    public boolean isApplicableToTokens() {
        return applicabilityType == PromoApplicabilityType.TOKEN 
                || applicabilityType == PromoApplicabilityType.ALL;
    }
}
