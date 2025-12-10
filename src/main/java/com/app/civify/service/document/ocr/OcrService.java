package com.app.civify.service.document.ocr;

import com.app.civify.dto.document.OcrResult;

/**
 * Interface for OCR (Optical Character Recognition) services.
 * Implementations can use different OCR providers (Gemini Vision, Tesseract, etc.).
 */
public interface OcrService {
    
    /**
     * Extract text from an image using OCR.
     *
     * @param imageData The image data as bytes
     * @param mimeType The MIME type of the image
     * @return OcrResult containing extracted text and metadata
     */
    OcrResult extractText(byte[] imageData, String mimeType);
    
    /**
     * Extract text from an image using OCR with a language hint.
     *
     * @param imageData The image data as bytes
     * @param mimeType The MIME type of the image
     * @param languageHint Optional language code (e.g., "ar", "en") for better accuracy
     * @return OcrResult containing extracted text and metadata
     */
    OcrResult extractText(byte[] imageData, String mimeType, String languageHint);
    
    /**
     * Check if this OCR service is available and properly configured.
     *
     * @return true if the service is available
     */
    boolean isAvailable();
    
    /**
     * Get the name of the OCR provider.
     *
     * @return Provider name (e.g., "GEMINI", "TESSERACT")
     */
    String getProviderName();
    
    /**
     * Get the priority of this OCR service (lower = higher priority).
     * Used for fallback ordering.
     *
     * @return Priority value
     */
    default int getPriority() {
        return 100;
    }
}

