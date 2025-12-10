package com.app.civify.service.document;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main service that orchestrates document processing. Delegates to appropriate
 * processors based on document type.
 */
@Service
@Slf4j
public class DocumentProcessorService {

    private final List<DocumentProcessor> processors;
    private final LanguageDetector languageDetector;

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/tiff",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    public DocumentProcessorService(List<DocumentProcessor> processors) {
        this.processors = processors;
        this.languageDetector = LanguageDetector.getDefaultLanguageDetector();
        try {
            this.languageDetector.loadModels();
        } catch (Exception e) {
            log.warn("Could not load language detection models: {}", e.getMessage());
        }
    }

    /**
     * Process a document file and extract text content.
     *
     * @param file The uploaded file
     * @return ProcessedDocument with extracted text and metadata
     */
    public ProcessedDocument process(MultipartFile file) {
        return process(file, null);
    }

    /**
     * Process a document file with a language hint.
     *
     * @param file The uploaded file
     * @param languageHint Optional language code for better accuracy
     * @return ProcessedDocument with extracted text and metadata
     */
    public ProcessedDocument process(MultipartFile file, String languageHint) {
        // Validate file
        validateFile(file);

        // Detect document type
        DocumentType documentType = detectDocumentType(file);
        log.info("Processing document: {}, type: {}", file.getOriginalFilename(), documentType);

        // Find appropriate processor
        DocumentProcessor processor = findProcessor(documentType);

        if (processor == null) {
            throw new BadRequestException("No processor available for document type: " + documentType);
        }

        // Process document
        ProcessedDocument result = processor.process(file, languageHint);

        // Detect language if not already detected
        if (result.hasText() && (result.getPrimaryLanguage() == null || result.getPrimaryLanguage().isEmpty())) {
            detectLanguage(result);
        }

        log.info("Document processed successfully: {}, method: {}, confidence: {}",
                file.getOriginalFilename(), result.getProcessingMethod(), result.getConfidence());

        return result;
    }

    /**
     * Validate the uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 20MB");
        }

        String contentType = file.getContentType();
        DocumentType type = detectDocumentType(file);

        if (type == DocumentType.UNKNOWN) {
            throw new BadRequestException("Unsupported file type: " + contentType
                    + ". Supported types: PDF, DOCX, PNG, JPEG, TIFF");
        }
    }

    /**
     * Detect document type from file.
     */
    private DocumentType detectDocumentType(MultipartFile file) {
        String contentType = file.getContentType();
        DocumentType type = DocumentType.fromMimeType(contentType);

        if (type == DocumentType.UNKNOWN) {
            type = DocumentType.fromExtension(file.getOriginalFilename());
        }

        return type;
    }

    /**
     * Find a processor that supports the given document type.
     */
    private DocumentProcessor findProcessor(DocumentType documentType) {
        return processors.stream()
                .filter(p -> p.supports(documentType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Detect language of the extracted text.
     */
    private void detectLanguage(ProcessedDocument document) {
        try {
            String text = document.getRawText();
            if (text == null || text.length() < 20) {
                return;
            }

            // Take a sample for detection
            String sample = text.substring(0, Math.min(text.length(), 1000));
            LanguageResult result = languageDetector.detect(sample);

            if (result.isReasonablyCertain()) {
                document.setPrimaryLanguage(result.getLanguage());
                List<String> languages = new ArrayList<>();
                languages.add(result.getLanguage());
                document.setDetectedLanguages(languages);
            }
        } catch (Exception e) {
            log.warn("Language detection failed: {}", e.getMessage());
        }
    }

    /**
     * Get list of supported file types.
     */
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    /**
     * Process a document from raw byte data.
     *
     * @param fileData The file data as bytes
     * @param mimeType The MIME type of the file
     * @param languageHint Optional language code for better accuracy
     * @return ProcessedDocument with extracted text and metadata
     */
    public ProcessedDocument processDocument(byte[] fileData, String mimeType, String languageHint) {
        if (fileData == null || fileData.length == 0) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage("File data is required and cannot be empty")
                    .build();
        }

        if (fileData.length > MAX_FILE_SIZE) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage("File size exceeds maximum allowed size of 20MB")
                    .build();
        }

        DocumentType documentType = DocumentType.fromMimeType(mimeType);
        if (documentType == DocumentType.UNKNOWN) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage("Unsupported file type: " + mimeType)
                    .build();
        }

        log.info("Processing document from bytes, type: {}, size: {} bytes", documentType, fileData.length);

        // Find appropriate processor
        DocumentProcessor processor = findProcessor(documentType);

        if (processor == null) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage("No processor available for document type: " + documentType)
                    .build();
        }

        try {
            // Create a mock MultipartFile from bytes
            MultipartFile mockFile = new ByteArrayMultipartFile(fileData, mimeType);
            ProcessedDocument result = processor.process(mockFile, languageHint);

            // Detect language if not already detected
            if (result.hasText() && (result.getPrimaryLanguage() == null || result.getPrimaryLanguage().isEmpty())) {
                detectLanguage(result);
            }

            return result;
        } catch (Exception e) {
            log.error("Error processing document", e);
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage("Error processing document: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Simple MultipartFile implementation for byte arrays.
     */
    private static class ByteArrayMultipartFile implements MultipartFile {

        private final byte[] content;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return "document";
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
