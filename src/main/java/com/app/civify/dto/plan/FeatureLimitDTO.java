package com.app.civify.dto.plan;

import com.app.civify.entity.enums.FeatureType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plan feature limits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureLimitDTO {
    
    private Long id;
    private Long featureId;
    private FeatureType featureType;
    private String featureName;
    private String featureDescription;
    private Integer limitValue; // null = unlimited
    private Boolean enabled;
    private String unit;
    private String metadata;
    private Integer displayOrder;
    private Boolean hasLimit;
}

