package com.app.civify.dto.plan;

import com.app.civify.entity.enums.IntegrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating integrations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationRequest {
    
    @NotNull(message = "Integration type is required")
    private IntegrationType integrationType;
    
    @NotBlank(message = "Integration name is required")
    private String integrationName;
    
    private String description;
    
    @NotNull(message = "Enabled status is required")
    private Boolean enabled = true;
    
    private Integer usageLimit; // null = unlimited
    private String usageUnit;
    private String configuration;
    private Integer displayOrder;
    private String iconUrl;
    private String documentationUrl;
}

