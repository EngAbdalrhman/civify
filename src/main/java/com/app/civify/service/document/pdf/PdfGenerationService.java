package com.app.civify.service.document.pdf;

import com.app.civify.dto.document.WatermarkConfigDto;
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
     * @param watermark Watermark overlay config (null = no watermark)
     * @return Byte array containing the generated PDF
     */
    byte[] generatePdf(ResumeData resumeData, String templateName, WatermarkConfigDto watermark);

    /** @param pdfUserId optional; forwarded to HICv for server-side PDF watermark fallback */
    default byte[] generatePdf(
            ResumeData resumeData,
            String templateName,
            WatermarkConfigDto watermark,
            Long pdfUserId) {
        return generatePdf(resumeData, templateName, watermark);
    }
}
