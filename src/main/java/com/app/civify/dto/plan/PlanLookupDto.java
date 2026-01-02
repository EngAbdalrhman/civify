package com.app.civify.dto.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO for plan lookup (dropdowns, filters, etc.)
 * Contains only essential fields for filtering
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plan lookup item for filtering")
public class PlanLookupDto {
    
    @Schema(description = "Plan ID", example = "1")
    private Long id;
    
    @Schema(description = "Plan name", example = "Premium")
    private String name;
}
