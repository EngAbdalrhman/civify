package com.app.civify.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPerCvPricingDto {
    private String productId;
    private String name;
    private String description;
    private BigDecimal basePriceUsd;
    private BigDecimal priceEgp;
    private BigDecimal exchangeRate;
    private String userCurrency;
    private String formattedDisplayPrice;
    private String formattedSecondaryPrice;
    private Integer credits;
    private Integer durationDays;
}
