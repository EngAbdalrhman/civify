package com.app.civify.dto.plan;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating feature limits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureLimitRequest {
    
    @NotNull(message = "Feature ID is required")
    private Long featureId;
    
    private Integer limitValue; // null = unlimited
    
    @Builder.Default
    @NotNull(message = "Enabled status is required")
    private Boolean enabled = true;
    
    private String metadata;
    private Integer displayOrder;
}

