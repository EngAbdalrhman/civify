package com.app.civify.service.document.masking;

import com.app.civify.dto.document.MaskRequest.MaskType;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Masking patterns for different types of PII in English and Arabic.
 */
@Getter
public enum MaskingPattern {

    // Email patterns
    EMAIL_ENGLISH(
            MaskType.EMAIL,
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
            "[EMAIL_REDACTED]",
            "en"
    ),
    // Phone patterns - International and Arabic numerals
    PHONE_INTERNATIONAL(
            MaskType.PHONE,
            Pattern.compile("(\\+?[0-9]{1,4}[\\s.-]?)?\\(?[0-9]{1,4}\\)?[\\s.-]?[0-9]{1,4}[\\s.-]?[0-9]{1,9}"),
            "[PHONE_REDACTED]",
            "en"
    ),
    PHONE_ARABIC(
            MaskType.PHONE,
            Pattern.compile("(\\+?[٠-٩0-9]{1,4}[\\s.-]?)?[٠-٩0-9]{6,14}"),
            "[رقم_الهاتف_محذوف]",
            "ar"
    ),
    // Address patterns
    ADDRESS_ENGLISH(
            MaskType.ADDRESS,
            Pattern.compile("\\d{1,5}\\s+[A-Za-z0-9\\s,.-]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln|Court|Ct|Way|Place|Pl)[.,]?\\s*(?:[A-Za-z\\s]+,?\\s*)?(?:[A-Z]{2})?\\s*\\d{0,5}"),
            "[ADDRESS_REDACTED]",
            "en"
    ),
    ADDRESS_ARABIC(
            MaskType.ADDRESS,
            Pattern.compile("(العنوان|عنوان|شارع|حي|مدينة|منطقة)[\\s:]*[\\u0600-\\u06FF\\s\\d,.-]+"),
            "[العنوان_محذوف]",
            "ar"
    ),
    // National ID patterns
    NATIONAL_ID_GENERIC(
            MaskType.NATIONAL_ID,
            Pattern.compile("\\b[0-9]{9,14}\\b"),
            "[ID_REDACTED]",
            "en"
    ),
    NATIONAL_ID_ARABIC(
            MaskType.NATIONAL_ID,
            Pattern.compile("(رقم الهوية|الهوية|رقم البطاقة)[\\s:]*[٠-٩0-9]{9,14}"),
            "[رقم_الهوية_محذوف]",
            "ar"
    ),
    // Date of birth patterns
    DOB_ENGLISH(
            MaskType.DATE_OF_BIRTH,
            Pattern.compile("(DOB|Date of Birth|Birth Date|Born)[\\s:]*\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}"),
            "[DOB_REDACTED]",
            "en"
    ),
    DOB_ARABIC(
            MaskType.DATE_OF_BIRTH,
            Pattern.compile("(تاريخ الميلاد|تاريخ الولادة|المواليد)[\\s:]*[٠-٩0-9]{1,2}[/.-][٠-٩0-9]{1,2}[/.-][٠-٩0-9]{2,4}"),
            "[تاريخ_الميلاد_محذوف]",
            "ar"
    ),
    // LinkedIn/Social profiles
    LINKEDIN(
            MaskType.SOCIAL_PROFILES,
            Pattern.compile("(?:https?://)?(?:www\\.)?linkedin\\.com/in/[a-zA-Z0-9_-]+/?"),
            "[LINKEDIN_REDACTED]",
            "en"
    ),
    GITHUB(
            MaskType.SOCIAL_PROFILES,
            Pattern.compile("(?:https?://)?(?:www\\.)?github\\.com/[a-zA-Z0-9_-]+/?"),
            "[GITHUB_REDACTED]",
            "en"
    ),
    // Photo placeholder (for PDF processing)
    PHOTO_MARKER(
            MaskType.PHOTO,
            Pattern.compile("\\[PHOTO\\]|\\[IMAGE\\]|\\[صورة\\]"),
            "[PHOTO_REDACTED]",
            "en"
    );

    private final MaskType maskType;
    private final Pattern pattern;
    private final String replacement;
    private final String language;

    MaskingPattern(MaskType maskType, Pattern pattern, String replacement, String language) {
        this.maskType = maskType;
        this.pattern = pattern;
        this.replacement = replacement;
        this.language = language;
    }

    /**
     * Check if this pattern applies to the given language.
     */
    public boolean appliesTo(String lang) {
        if (lang == null) {
            return true;
        }
        return language.equals("en") || language.equalsIgnoreCase(lang);
    }
}
