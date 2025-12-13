package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of OCR (Optical Character Recognition) processing.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcrResult {
    
    private String text;
    
    @Builder.Default
    private double confidence = 0.0;
    
    private String detectedLanguage;
    
    @Builder.Default
    private List<String> detectedLanguages = new ArrayList<>();
    
    private String ocrProvider;
    
    private long processingTimeMs;
    
    @Builder.Default
    private boolean success = false;
    
    private String errorMessage;
    
    @Builder.Default
    private List<TextBlock> textBlocks = new ArrayList<>();
    
    public boolean hasText() {
        return text != null && !text.trim().isEmpty();
    }
    
    public boolean isHighConfidence() {
        return confidence >= 0.85;
    }
    
    public static OcrResult empty() {
        return OcrResult.builder()
                .text("")
                .confidence(0.0)
                .success(false)
                .build();
    }
    
    public static OcrResult error(String errorMessage) {
        return OcrResult.builder()
                .text("")
                .confidence(0.0)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    public static OcrResult success(String text, double confidence, String provider) {
        return OcrResult.builder()
                .text(text)
                .confidence(confidence)
                .success(true)
                .ocrProvider(provider)
                .build();
    }
    
    /**
     * Represents a block of text with position information (for advanced processing).
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TextBlock {
        private String text;
        private int x;
        private int y;
        private int width;
        private int height;
        private double confidence;
        private int pageNumber;
    }
}

