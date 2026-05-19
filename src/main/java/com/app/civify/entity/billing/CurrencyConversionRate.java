package com.app.civify.entity.billing;

import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Stores static currency conversion rates for multi-gateway pricing.
 * Rates are managed manually in the database (no admin UI for now).
 * Fallback to application.properties if no DB row exists.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "currency_conversion_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency"}))
public class CurrencyConversionRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;
}
