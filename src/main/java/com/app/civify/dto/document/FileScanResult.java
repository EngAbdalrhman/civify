package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a file security scan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileScanResult {

    /** True if the file passed all security checks. */
    private boolean clean;

    /** Threat category identifier, e.g. "JAVASCRIPT_IN_PDF", "VBA_MACRO". Null when clean. */
    private String threatType;

    /** Human-readable description of the detected threat. Null when clean. */
    private String details;

    public static FileScanResult safe() {
        return FileScanResult.builder().clean(true).build();
    }

    public static FileScanResult threat(String threatType, String details) {
        return FileScanResult.builder()
                .clean(false)
                .threatType(threatType)
                .details(details)
                .build();
    }
}
