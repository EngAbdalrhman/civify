package com.app.civify.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for CV masking operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaskRequest {

    /**
     * Text content to mask (for text-only masking).
     */
    private String text;

    /**
     * Optional language hint for better masking accuracy.
     */
    private String language;

    /**
     * Types of information to mask.
     */
    @Builder.Default
    private List<MaskType> maskTypes = new ArrayList<>();

    /**
     * Whether to generate a masked PDF output.
     */
    @Builder.Default
    private boolean generatePdf = true;

    /**
     * Custom replacement text for names.
     */
    private String nameReplacement;

    /**
     * Custom replacement text for emails.
     */
    private String emailReplacement;

    /**
     * Custom replacement text for phones.
     */
    private String phoneReplacement;

    /**
     * Enum for types of information to mask.
     */
    public enum MaskType {
        NAME,
        EMAIL,
        PHONE,
        ADDRESS,
        SOCIAL_PROFILES,
        DATE_OF_BIRTH,
        NATIONAL_ID,
        PHOTO,
        ALL
    }

    /**
     * Check if a specific type should be masked.
     */
    public boolean shouldMask(MaskType type) {
        if (maskTypes == null || maskTypes.isEmpty()) {
            return true; // Mask all by default
        }
        return maskTypes.contains(MaskType.ALL) || maskTypes.contains(type);
    }
}
