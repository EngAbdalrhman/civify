package com.app.civify.dto.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a new subscription plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCreateRequest {
    
    @NotBlank(message = "Plan name is required")
    private String name;
    
    private String description;
    private String details;
    
    // Legacy fields (optional, for backward compatibility)
    private Integer cvLimit;
    private Integer apiLimit;
    private Boolean watermark;
    private Boolean dashboard;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    
    @NotNull(message = "Active status is required")
    private Boolean active = true;
    
    // New enhanced fields
    @Valid
    private List<FeatureLimitRequest> features;
    
    @Valid
    private List<PricingRequest> pricingOptions;
    
    @Valid
    private List<DiscountRequest> discounts;
    
    @Valid
    private List<IntegrationRequest> integrations;
}

