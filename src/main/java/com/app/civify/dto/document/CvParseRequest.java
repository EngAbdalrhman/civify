package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for CV parsing operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CvParseRequest {
    
    /**
     * Optional language hint for better parsing accuracy.
     * If not provided, language will be auto-detected.
     * Examples: "en", "ar", "fr"
     */
    private String language;
    
    /**
     * Whether to save the parsed resume to the database.
     */
    @Builder.Default
    private boolean saveToDatabase = true;
    
    /**
     * Optional title for the resume (used when saving).
     */
    private String resumeTitle;
    
    /**
     * Whether to force OCR processing even for text-based PDFs.
     */
    @Builder.Default
    private boolean forceOcr = false;
    
    /**
     * Whether to include extracted images in the response.
     */
    @Builder.Default
    private boolean includeImages = false;
}

