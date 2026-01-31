package com.app.civify.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Simplified DTO for subscription plans (public API response).
 * Avoids circular references from entity relationships.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimplePlanDTO {

    private Long id;
    private String name;
    private String description;
    private String details;
    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private Integer cvLimit;
    private Boolean watermark;
    private Boolean active;
    private Boolean allowAnnualBilling;
    private Boolean renewalAllowed;
    private Integer sortOrder;
    private Boolean hasApiAccess;
}
