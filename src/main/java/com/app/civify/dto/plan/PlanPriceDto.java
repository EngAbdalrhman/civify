package com.app.civify.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for plan price in a specific currency
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPriceDto {
    private Long planId;
    private String planName;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private String currencyCode;
    private String currencySymbol;
    private boolean isOverride; // true if using override price, false if converted
}
