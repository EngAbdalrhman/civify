package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of document processing/text extraction.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedDocument {

    @Builder.Default
    private boolean success = true;

    private String errorMessage;

    private String rawText;

    private DocumentType documentType;

    private String processingMethod;

    private int pageCount;

    @Builder.Default
    private double confidence = 1.0;

    @Builder.Default
    private List<String> detectedLanguages = new ArrayList<>();

    private String primaryLanguage;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<byte[]> extractedImages = new ArrayList<>();

    private long processingTimeMs;

    private String originalFilename;

    private long fileSizeBytes;

    public boolean hasText() {
        return rawText != null && !rawText.trim().isEmpty();
    }

    public boolean isHighConfidence() {
        return confidence >= 0.85;
    }

    public boolean isArabic() {
        return "ar".equalsIgnoreCase(primaryLanguage)
                || (detectedLanguages != null && detectedLanguages.contains("ar"));
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
}
