package com.app.civify.dto.jobmatch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import com.app.civify.dto.resume.ResumeData;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchRequest {
    @NotNull(message = "Resume data is required")
    private ResumeData resumeData;

    private String resumeId;

    // Provide one of the below:
    private String jobDescription;
    private String jobUrl;
}
