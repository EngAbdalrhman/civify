package com.app.civify.dto.plan;

import com.app.civify.util.security.EncodedIdField;
import com.app.civify.util.security.EncodedIdSerializer;
import com.app.civify.util.security.EntityType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

    @JsonSerialize(using = EncodedIdSerializer.class)
    @EncodedIdField(EntityType.PLAN)
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
}
