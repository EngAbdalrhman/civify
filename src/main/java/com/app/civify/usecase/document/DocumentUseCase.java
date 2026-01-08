package com.app.civify.usecase.document;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskResponse;
import com.app.civify.dto.document.ProcessedDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Use case interface for document processing operations.
 */
public interface DocumentUseCase {

    /**
     * Parse a CV/Resume file.
     *
     * @param file the file to parse
     * @param request parse request options
     * @param userId the authenticated user ID
     * @return parsed CV response
     */
    CvParseResponse parseCv(MultipartFile file, CvParseRequest request, Long userId);

    /**
     * Extract text from a document.
     *
     * @param file the file to extract text from
     * @param languageHint optional language hint
     * @param userId the authenticated user ID (for token charging)
     * @return processed document with extracted text
     */
    ProcessedDocument extractText(MultipartFile file, String languageHint, Long userId);

    /**
     * Mask PII in a document file.
     *
     * @param file the file to mask
     * @param request mask request options
     * @param userId the authenticated user ID (for token charging)
     * @return mask response
     */
    MaskResponse maskDocument(MultipartFile file, MaskRequest request, Long userId);

    /**
     * Mask PII in plain text.
     *
     * @param request mask request with text
     * @param userId the authenticated user ID (for token charging)
     * @return mask response
     */
    MaskResponse maskText(MaskRequest request, Long userId);

    /**
     * Get supported MIME types for document processing.
     *
     * @return set of supported MIME types
     */
    Set<String> getSupportedMimeTypes();
}
