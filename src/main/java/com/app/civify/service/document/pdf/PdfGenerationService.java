package com.app.civify.service.document.pdf;

import com.app.civify.dto.resume.ResumeData;

/**
 * Service for generating PDFs from structured resume data.
 */
public interface PdfGenerationService {
    
    /**
     * Generate a PDF from resume data using a specific template.
     * 
     * @param resumeData The structured resume data
     * @param templateName The name of the template to use (e.g., "modern", "classic")
     * @return Byte array containing the generated PDF
     */
    byte[] generatePdf(ResumeData resumeData, String templateName);
}
