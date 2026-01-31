package com.app.civify.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified DTO for plan features (public API response).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimpleFeatureDTO {

    private String code;
    private String name;
    private Integer limitValue;
    private Boolean enabled;
}
