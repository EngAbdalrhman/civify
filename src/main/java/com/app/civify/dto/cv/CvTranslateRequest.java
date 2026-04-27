package com.app.civify.dto.cv;

import com.app.civify.dto.resume.ResumeData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CvTranslateRequest {
    @NotNull 
    private ResumeData resumeData;
    
    @NotBlank 
    private String targetLanguage; // ISO 639-1 code (e.g. "ar", "fr", "de")
}
