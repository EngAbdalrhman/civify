package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatermarkConfigDto {
    private boolean enabled;
    private String imageUrl;
    private Double opacity;
    private Double scale;
    private String position;
    private Double rotation;
}
