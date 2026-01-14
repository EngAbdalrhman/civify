package com.app.civify.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed response DTO for subscription plans
 * Includes all features, pricing, discounts, and integrations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanDetailResponse {
    
    private Long id;
    private String name;
    private String description;
    private String details;
    
    private Integer cvLimit;
    private Integer tokenLimit;
    private Boolean watermark;
    private Boolean dashboard;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private String currency;
    
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Billing restrictions
    private Boolean allowAnnualBilling;
    private Boolean renewalAllowed;
    private Integer maxBillingCycles;
    
    // Display settings
    private Boolean isFeatured;
    private Integer sortOrder;
    
    // Linked group
    private Long linkedGroupId;
    private String linkedGroupName;
    
    // New enhanced fields
    private List<FeatureLimitDTO> features;
    private List<PricingDTO> pricingOptions;
    private List<DiscountDTO> discounts;
    private List<IntegrationDTO> integrations;
    
    // Statistics (for admin)
    private Long activeSubscriptionCount;
}

