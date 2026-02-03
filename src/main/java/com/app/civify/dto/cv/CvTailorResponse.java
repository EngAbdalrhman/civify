package com.app.civify.dto.cv;

import com.app.civify.dto.resume.ResumeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for CV tailoring operations.
 * Wrapped with CommonResponse for consistency.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CvTailorResponse {

    /**
     * The tailored resume data optimized for the job description.
     */
    private ResumeData tailoredCv;

    /**
     * ATS (Applicant Tracking System) analysis for the TAILORED CV.
     */
    private AtsScore atsScore;

    /**
     * ATS analysis for the ORIGINAL CV before tailoring.
     * Used for showing improvement (before/after comparison).
     */
    private AtsScore originalAtsScore;

    /**
     * Interview preparation materials. Null if not requested.
     */
    private InterviewPrep interviewPrep;

    /**
     * Career roadmap. Null if not requested.
     */
    private Roadmap roadmap;

    /**
     * Generated cover letter. Null if not requested.
     */
    private String coverLetter;
    
    /**
     * Visual feedback for what changed in the CV.
     */
    private List<String> changes;
    
    /**
     * Warnings about potential hallucinations or alignment issues.
     */
    private List<String> validationWarnings;

    /**
     * Total processing time in milliseconds.
     */
    private long processingTimeMs;

    // ==================== Nested DTOs ====================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AtsScore {
        /**
         * Overall ATS match score (0-100).
         */
        private int overall;

        /**
         * Keywords from JD found in CV (0-100).
         */
        private int keywordMatch;

        /**
         * Skills alignment score (0-100).
         */
        private int skillsMatch;

        /**
         * Keywords from job description missing in CV.
         */
        private List<String> missingKeywords;

        /**
         * Improvement suggestions.
         */
        private List<String> suggestions;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InterviewPrep {
        /**
         * List of potential interview questions.
         */
        private List<InterviewQuestion> questions;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InterviewQuestion {
        private String question;
        private String hint;
        private String category; // e.g., "behavioral", "technical", "situational"
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Roadmap {
        /**
         * Skills to develop to better match the role.
         */
        private List<String> skillGaps;

        /**
         * Actionable steps with timeframes.
         */
        private List<RoadmapStep> steps;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoadmapStep {
        private String step;
        private String timeframe; // e.g., "1-2 weeks", "1 month"
    }
}
