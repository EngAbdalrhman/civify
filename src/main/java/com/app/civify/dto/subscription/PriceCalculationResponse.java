package com.app.civify.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponse {
    private Long planId;
    private String planName;
    private BigDecimal basePrice;
    private BigDecimal discount;
    private BigDecimal totalPrice;
    private String promoCode;
    private boolean isAnnual;
}
