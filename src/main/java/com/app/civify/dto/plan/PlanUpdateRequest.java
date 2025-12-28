package com.app.civify.dto.plan;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating an existing subscription plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpdateRequest {
    
    private String name;
    private String description;
    private String details;
    
    private Integer cvLimit;
    private Boolean watermark;
    private Boolean dashboard;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    
    private Boolean active;
    
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

