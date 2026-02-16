package com.app.civify.dto.document;

import com.app.civify.dto.resume.ResumeData;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for CV parsing operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CvParseResponse {
    
    private boolean success;
    
    private String message;
    
    /**
     * The parsed and structured resume data.
     */
    private ResumeData resumeData;
    
    /**
     * The ID of the saved resume (if saved to database).
     */
    private String resumeId;
    
    /**
     * Detected primary language of the document.
     */
    private String detectedLanguage;
    
    /**
     * All detected languages in the document.
     */
    @Builder.Default
    private List<String> detectedLanguages = new ArrayList<>();
    
    /**
     * Processing method used (DIRECT_EXTRACTION, OCR, etc.).
     */
    private String processingMethod;
    
    /**
     * Confidence score of the extraction (0.0 to 1.0).
     */
    private double confidence;
    
    /**
     * Machine-readable error code for frontend i18n translation.
     * Examples: TOKEN_LIMIT_EXCEEDED, INVALID_FILE_TYPE
     */
    private String errorCode;

    /**
     * Total tokens used for the parsing operation.
     */
    private int tokensUsed;
    
    /**
     * Total processing time in milliseconds.
     */
    private long processingTimeMs;
    
    /**
     * Any warnings generated during processing.
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    public static CvParseResponse success(ResumeData resumeData, String resumeId) {
        return CvParseResponse.builder()
                .success(true)
                .message("CV parsed successfully")
                .resumeData(resumeData)
                .resumeId(resumeId)
                .build();
    }
    
    public static CvParseResponse error(String message) {
        return CvParseResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}

