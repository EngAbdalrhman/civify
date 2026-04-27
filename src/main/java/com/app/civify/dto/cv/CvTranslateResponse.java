package com.app.civify.dto.cv;

import com.app.civify.dto.resume.ResumeData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CvTranslateResponse {
    private ResumeData translatedCv;
    private String sourceLanguage;
    private String targetLanguage;
    private int tokensUsed;
    private long processingTimeMs;
}
