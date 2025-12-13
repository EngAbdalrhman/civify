package com.app.civify.service.document.masking;

import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskResponse;

/**
 * Service interface for masking PII (Personally Identifiable Information) in documents.
 */
public interface MaskingService {
    
    /**
     * Mask PII in the given text based on the mask request configuration.
     *
     * @param text The text to mask
     * @param request The mask request containing configuration
     * @return MaskResponse with masked text and details of masked entities
     */
    MaskResponse maskText(String text, MaskRequest request);
    
    /**
     * Mask PII in a document file.
     *
     * @param fileData The document file data
     * @param mimeType The MIME type of the document
     * @param request The mask request containing configuration
     * @return MaskResponse with masked text and optionally masked PDF
     */
    MaskResponse maskDocument(byte[] fileData, String mimeType, MaskRequest request);
    /**
     * Mask sensitive information in structured ResumeData.
     *
     * @param resumeData The structured resume data
     * @param request Masking configuration
     * @return The masked ResumeData
     */
    com.app.civify.dto.resume.ResumeData maskResumeData(com.app.civify.dto.resume.ResumeData resumeData, com.app.civify.dto.document.MaskRequest request);
}

