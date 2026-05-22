package com.app.civify.dto.watermark;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformWatermarkSettingsDto {
    private String logoStorageKey;
    private String logoUrl;
    private Double opacity;
    private Double scale;
    private String position;
    private Double rotation;
    private Boolean enabled;
}
