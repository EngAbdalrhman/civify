package com.app.civify.dto.watermark;

import lombok.Data;

@Data
public class WatermarkSettingsUpdateRequest {
    private Double opacity;
    private Double scale;
    private String position;
    private Double rotation;
    private Boolean enabled;
    private Boolean usePlatformWatermark;
}
