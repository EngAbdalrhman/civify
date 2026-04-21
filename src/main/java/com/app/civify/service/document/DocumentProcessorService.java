package com.app.civify.service.document;

import com.app.civify.dto.document.DocumentType;
import com.app.civify.dto.document.FileScanResult;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.MaliciousFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final com.app.civify.service.i18n.MessageService messageService;
    private final FileScannerService fileScannerService;

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

    public DocumentProcessorService(List<DocumentProcessor> processors, com.app.civify.service.i18n.MessageService messageService, FileScannerService fileScannerService) {
        this.processors = processors;
        this.messageService = messageService;
        this.fileScannerService = fileScannerService;

        LanguageDetector detector = null;
        try {
            detector = new OptimaizeLangDetector().loadModels();
        } catch (Exception e) {
            log.warn("Could not load language detection models: {}", e.getMessage());
            // Fallback to a dummy detector if models fail to load, to prevent app crash
            detector = new LanguageDetector() {
                @Override
                public LanguageDetector loadModels() { return this; }
                @Override
                public LanguageDetector loadModels(Set<String> languages) { return this; }
                @Override
                public boolean hasModel(String language) { return false; }

                @Override
                public LanguageDetector setPriors(Map<String, Float> map) throws IOException {
                    return null;
                }

                @Override
                public LanguageResult detect(CharSequence text) { return new LanguageResult("en", org.apache.tika.language.detect.LanguageConfidence.LOW, 0.0f); }
                @Override
                public void addText(char[] cbuf, int off, int len) {}
                @Override
                public List<LanguageResult> detectAll() { return List.of(); }
                @Override
                public void reset() {}
            };
        }
        this.languageDetector = detector;
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
            throw new BadRequestException(messageService.getMessage("document.processor.notfound", new Object[]{documentType}));
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
            throw new BadRequestException("document.file.required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("document.file.sizes");
        }

        String contentType = file.getContentType();
        DocumentType type = detectDocumentType(file);

        if (type == DocumentType.UNKNOWN) {
            throw new BadRequestException(messageService.getMessage("document.file.type", new Object[]{contentType}));
        }

        // Security scan: check for malware/threats before any processing
        FileScanResult scanResult = fileScannerService.scanFile(file);
        if (!scanResult.isClean()) {
            log.warn("File '{}' rejected by security scan: {} - {}",
                    file.getOriginalFilename(), scanResult.getThreatType(), scanResult.getDetails());
            throw new MaliciousFileException(
                    messageService.getMessage("document.file.malicious", new Object[]{scanResult.getThreatType()}),
                    scanResult.getThreatType());
        }
    }

    /**
     * Detect document type from file.
     */
    private DocumentType detectDocumentType(MultipartFile file) {
        // 1) Magic number detection (most trustworthy)
        DocumentType type = detectByMagicNumbers(file);
        if (type != DocumentType.UNKNOWN) {
            return type;
        }

        // 2) Reported content type
        String contentType = file.getContentType();
        type = DocumentType.fromMimeType(contentType);
        if (type != DocumentType.UNKNOWN) {
            return type;
        }

        // 3) Fallback to extension
        return DocumentType.fromExtension(file.getOriginalFilename());
    }

    private DocumentType detectByMagicNumbers(MultipartFile file) {
        try (var is = new java.io.BufferedInputStream(file.getInputStream())) {
            if (!is.markSupported()) {
                // Should not happen with BufferedInputStream, but safe check
                log.warn("Mark/Reset not supported even with BufferedInputStream");
                return DocumentType.UNKNOWN;
            }
            is.mark(32);
            byte[] header = is.readNBytes(16);
            is.reset();

            if (startsWith(header, "%PDF-".getBytes())) {
                return DocumentType.PDF;
            }
            if (header.length >= 4 && header[0] == (byte) 0x50 && header[1] == (byte) 0x4B) {
                // ZIP container -> likely DOCX
                return DocumentType.DOCX;
            }
            if (startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8})) {
                return DocumentType.IMAGE_JPEG;
            }
            if (startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) {
                return DocumentType.IMAGE_PNG;
            }
            if (startsWith(header, new byte[]{0x49, 0x49, 0x2A, 0x00}) ||
                startsWith(header, new byte[]{0x4D, 0x4D, 0x00, 0x2A})) {
                return DocumentType.IMAGE_TIFF;
            }
            if (header.length >= 12 &&
                    startsWith(header, "RIFF".getBytes()) &&
                    header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return DocumentType.IMAGE_WEBP;
            }
        } catch (Exception e) {
            log.warn("Magic number detection failed: {}", e.getMessage());
        }
        return DocumentType.UNKNOWN;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data == null || prefix == null || data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
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
                    .errorMessage(messageService.getMessage("document.file.required"))
                    .build();
        }

        if (fileData.length > MAX_FILE_SIZE) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage(messageService.getMessage("document.file.sizes"))
                    .build();
        }

        DocumentType documentType = DocumentType.fromMimeType(mimeType);
        if (documentType == DocumentType.UNKNOWN) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage(messageService.getMessage("document.file.type", new Object[]{mimeType}))
                    .build();
        }

        // Security scan for raw byte uploads
        FileScanResult scanResult = fileScannerService.scanBytes(fileData, mimeType);
        if (!scanResult.isClean()) {
            log.warn("Raw byte upload rejected by security scan: {} - {}",
                    scanResult.getThreatType(), scanResult.getDetails());
            throw new MaliciousFileException(
                    messageService.getMessage("document.file.malicious", new Object[]{scanResult.getThreatType()}),
                    scanResult.getThreatType());
        }

        log.info("Processing document from bytes, type: {}, size: {} bytes", documentType, fileData.length);

        // Find appropriate processor
        DocumentProcessor processor = findProcessor(documentType);

        if (processor == null) {
            return ProcessedDocument.builder()
                    .success(false)
                    .errorMessage(messageService.getMessage("document.processor.notfound", new Object[]{documentType}))
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