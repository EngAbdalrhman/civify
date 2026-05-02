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
    private Integer jobMatchLimit;
    private Integer tokenLimit;
    private Boolean watermark;
    private Boolean dashboard;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    
    private Boolean active;
    
    // Linked group
    private Long linkedGroupId;
    
    // Billing restrictions
    private Boolean allowAnnualBilling;
    private Boolean renewalAllowed;
    private Integer maxBillingCycles;
    
    // Display settings
    private Boolean isFeatured;
    private Integer sortOrder;
    private Boolean hasApiAccess;
    private Boolean allowTranslation;
    private Boolean allowDocExport;
    
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

