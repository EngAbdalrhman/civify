package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for CV masking operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaskResponse {

    private boolean success;

    private String message;

    private String errorMessage;

    /**
     * The masked text content.
     */
    private String maskedText;

    /**
     * Base64-encoded masked PDF (if generatePdf was true).
     */
    private String maskedPdfBase64;

    /**
     * Number of entities that were masked.
     */
    private int maskedEntitiesCount;

    /**
     * Details of what was masked.
     */
    @Builder.Default
    private List<MaskedEntity> maskedEntities = new ArrayList<>();

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;

    /**
     * Detected language of the document.
     */
    private String detectedLanguage;

    /**
     * Represents a single masked entity.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaskedEntity {

        private String type;
        private String originalValue;
        private String maskedValue;
        private int startIndex;
        private int endIndex;
    }

    public static MaskResponse success(String maskedText, List<MaskedEntity> entities) {
        return MaskResponse.builder()
                .success(true)
                .message("CV masked successfully")
                .maskedText(maskedText)
                .maskedEntities(entities)
                .maskedEntitiesCount(entities != null ? entities.size() : 0)
                .build();
    }

    public static MaskResponse error(String message) {
        return MaskResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
