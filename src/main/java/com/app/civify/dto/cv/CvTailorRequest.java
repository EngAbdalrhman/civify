package com.app.civify.dto.cv;

import com.app.civify.dto.resume.ResumeData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for CV tailoring operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CvTailorRequest {

    /**
     * The parsed resume data, possibly edited by user.
     */
    @NotNull(message = "Resume data is required")
    private ResumeData resumeData;

    /**
     * Target job description text.
     */
    @NotBlank(message = "Job description is required")
    private String jobDescription;

    /**
     * Optional job title for better keyword matching.
     */
    private String jobTitle;

    /**
     * Optional company name for personalization.
     */
    private String companyName;

    /**
     * Whether to generate a cover letter. Default: false.
     */
    @Builder.Default
    private boolean generateCoverLetter = false;

    /**
     * Whether to include interview questions. Default: false.
     */
    @Builder.Default
    private boolean includeInterviewQuestions = false;

    /**
     * Whether to include career roadmap. Default: false.
     */
    @Builder.Default
    private boolean includeRoadmap = false;
}
