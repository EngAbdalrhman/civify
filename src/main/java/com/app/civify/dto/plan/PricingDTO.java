package com.app.civify.dto.plan;

import com.app.civify.entity.enums.BillingCycle;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for plan pricing options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PricingDTO {
    
    private Long id;
    private BillingCycle billingCycle;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discountPercentage;
    private String currency;
    private Boolean active;
    private Integer displayOrder;
    private Boolean recommended;
    private BigDecimal effectiveMonthlyPrice; // Calculated field
    private BigDecimal savings; // Calculated field (compared to monthly)
}

