package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }

    public boolean isValid() {
        return active
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()))
                && (usageLimit == null || usedCount < usageLimit);
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (!isValid()) {
            return BigDecimal.ZERO;
        }

        if (discountType == DiscountType.PERCENTAGE) {
            return amount.multiply(discountValue);
        } else {
            return discountValue.min(amount); // Don't exceed purchase amount
        }
    }
}
