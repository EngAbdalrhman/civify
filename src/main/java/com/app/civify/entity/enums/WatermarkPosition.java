package com.app.civify.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Where the PDF watermark logo is placed on the page.
 */
public enum WatermarkPosition {
    CENTER("center", "Center (large overlay)"),
    TOP_LEFT("top-left", "Top left (header area)"),
    TOP_RIGHT("top-right", "Top right (header area)"),
    BOTTOM_LEFT("bottom-left", "Bottom left (footer area)"),
    BOTTOM_RIGHT("bottom-right", "Bottom right (footer area)");

    private final String value;
    private final String label;

    WatermarkPosition(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static WatermarkPosition fromValue(String value) {
        if (value == null || value.isBlank()) {
            return CENTER;
        }
        for (WatermarkPosition p : values()) {
            if (p.value.equalsIgnoreCase(value.trim())) {
                return p;
            }
        }
        return CENTER;
    }
}
