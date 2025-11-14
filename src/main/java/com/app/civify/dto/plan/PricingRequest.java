package com.app.civify.dto.plan;

import com.app.civify.entity.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating/updating pricing options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRequest {
    
    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;
    
    private BigDecimal originalPrice;
    private BigDecimal discountPercentage;
    
    private String currency = "USD";
    
    @NotNull(message = "Active status is required")
    private Boolean active = true;
    
    private Integer displayOrder;
    private Boolean recommended = false;
}

