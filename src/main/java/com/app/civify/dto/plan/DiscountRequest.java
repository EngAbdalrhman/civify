package com.app.civify.dto.plan;

import com.app.civify.entity.enums.BillingCycle;
import com.app.civify.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating/updating discounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequest {
    
    @NotBlank(message = "Discount name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Discount type is required")
    private DiscountType discountType;
    
    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", message = "Discount value must be non-negative")
    private BigDecimal discountValue;
    
    private BillingCycle billingCycle; // null = applies to all cycles
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @NotNull(message = "Active status is required")
    private Boolean active = true;
    
    private Integer maxUses;
    private Integer priority = 0;
}

