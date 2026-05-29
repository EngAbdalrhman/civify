package com.app.civify.dto.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic ATS format and parseability analysis for a resume.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AtsFormatReport {

    private int formatScore;
    private int parseabilityScore;
    private String atsReadiness;
    private String recommendedTemplateId;
    @Builder.Default
    private List<AtsFormatIssue> issues = new ArrayList<>();
    @Builder.Default
    private List<String> exportHints = new ArrayList<>();
}
