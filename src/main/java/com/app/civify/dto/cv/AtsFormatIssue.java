package com.app.civify.dto.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single ATS format or parseability issue detected in resume metadata or structure.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AtsFormatIssue {

    public enum Severity {
        INFO, WARNING, CRITICAL
    }

    private String code;
    private Severity severity;
    private String message;
    private String fixHint;
}
