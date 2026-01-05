package com.app.civify.entity.currency;

import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a supported currency with exchange rate
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "currencies")
@EqualsAndHashCode(callSuper = true)
public class Currency extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code; // EGP, USD, SAR, AED

    @Column(nullable = false, length = 50)
    private String name; // Egyptian Pound, US Dollar, etc.

    @Column(length = 10)
    private String symbol; // E£, $, ر.س, د.إ

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rateToUsd; // Exchange rate relative to USD

    /**
     * Country codes that use this currency (ISO 3166-1 alpha-2)
     * e.g., ["EG"] for EGP, ["SA"] for SAR, ["US", "EC", "SV"] for USD
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> countryCodes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Check if this currency is used in the given country
     */
    public boolean isUsedInCountry(String countryCode) {
        if (countryCodes == null || countryCode == null) return false;
        return countryCodes.stream()
                .anyMatch(c -> c.equalsIgnoreCase(countryCode));
    }

    /**
     * Convert an amount from this currency to USD
     */
    public BigDecimal toUsd(BigDecimal amount) {
        if (amount == null || rateToUsd == null || rateToUsd.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        return amount.divide(rateToUsd, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Convert an amount from USD to this currency
     */
    public BigDecimal fromUsd(BigDecimal amountInUsd) {
        if (amountInUsd == null || rateToUsd == null) {
            return amountInUsd;
        }
        return amountInUsd.multiply(rateToUsd).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
