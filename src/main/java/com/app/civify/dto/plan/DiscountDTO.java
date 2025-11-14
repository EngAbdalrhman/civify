package com.app.civify.dto.plan;

import com.app.civify.entity.enums.BillingCycle;
import com.app.civify.entity.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for plan discounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscountDTO {
    
    private Long id;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BillingCycle billingCycle; // null = applies to all cycles
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;
    private Integer maxUses;
    private Integer currentUses;
    private Integer priority;
    private Boolean isValid; // Calculated field
}

