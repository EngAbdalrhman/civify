package com.app.civify.dto.cv;

import com.app.civify.dto.resume.ResumeData;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for general ATS score calculation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CvScoreRequest {

    /**
     * The resume data to analyze for ATS compatibility.
     */
    @NotNull(message = "Resume data is required")
    private ResumeData resumeData;
}
