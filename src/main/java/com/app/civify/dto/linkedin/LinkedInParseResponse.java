package com.app.civify.dto.linkedin;

import com.app.civify.dto.resume.ResumeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInParseResponse {
    private ResumeData resumeData;
    private String detectedName;
    private String detectedHeadline;
    private int experienceCount;
    private int educationCount;
    private int skillsCount;
    private boolean success;
    private String errorMessage;
}
