package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Price override for a plan in a specific currency.
 * If no override exists, price is calculated from USD using exchange rate.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plan_currency_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "currency_code"}))
@EqualsAndHashCode(callSuper = true)
public class PlanCurrencyPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private SubscriptionPlan plan;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode; // EGP, USD, SAR, AED

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal annualPrice;
}
