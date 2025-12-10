package com.app.civify.service.document;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.ProcessedDocument;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for document processing operations.
 * Implementations handle specific document types (PDF, DOCX, images).
 */
public interface DocumentProcessor {
    
    /**
     * Process a document and extract text content.
     *
     * @param file The uploaded file to process
     * @return ProcessedDocument containing extracted text and metadata
     */
    ProcessedDocument process(MultipartFile file);
    
    /**
     * Process a document with a language hint for better accuracy.
     *
     * @param file The uploaded file to process
     * @param languageHint Optional language code (e.g., "ar", "en")
     * @return ProcessedDocument containing extracted text and metadata
     */
    ProcessedDocument process(MultipartFile file, String languageHint);
    
    /**
     * Check if this processor supports the given document type.
     *
     * @param documentType The document type to check
     * @return true if this processor can handle the document type
     */
    boolean supports(DocumentType documentType);
    
    /**
     * Check if this processor supports the given file.
     *
     * @param file The file to check
     * @return true if this processor can handle the file
     */
    default boolean supports(MultipartFile file) {
        DocumentType type = detectDocumentType(file);
        return supports(type);
    }
    
    /**
     * Detect the document type from the file.
     *
     * @param file The file to analyze
     * @return The detected DocumentType
     */
    default DocumentType detectDocumentType(MultipartFile file) {
        String contentType = file.getContentType();
        DocumentType type = DocumentType.fromMimeType(contentType);
        
        if (type == DocumentType.UNKNOWN) {
            type = DocumentType.fromExtension(file.getOriginalFilename());
        }
        
        return type;
    }
}

