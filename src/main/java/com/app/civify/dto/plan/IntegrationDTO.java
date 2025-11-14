package com.app.civify.dto.plan;

import com.app.civify.entity.enums.IntegrationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plan integrations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationDTO {
    
    private Long id;
    private IntegrationType integrationType;
    private String integrationName;
    private String description;
    private Boolean enabled;
    private Integer usageLimit; // null = unlimited
    private String usageUnit;
    private String configuration;
    private Integer displayOrder;
    private String iconUrl;
    private String documentationUrl;
    private Boolean isUnlimited; // Calculated field
}

