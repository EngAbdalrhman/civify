package com.app.civify.service.document.masking;

import com.app.civify.dto.document.MaskRequest;
import com.app.civify.dto.document.MaskRequest.MaskType;
import com.app.civify.dto.document.MaskResponse;
import com.app.civify.dto.document.MaskResponse.MaskedEntity;
import com.app.civify.service.document.DocumentProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Implementation of MaskingService that uses pattern-based and AI-assisted
 * masking.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaskingServiceImpl implements MaskingService {

    private final DocumentProcessorService documentProcessorService;

    @Override
    public MaskResponse maskText(String text, MaskRequest request) {
        if (text == null || text.isEmpty()) {
            return MaskResponse.builder()
                    .maskedText("")
                    .maskedEntities(Collections.emptyList())
                    .success(true)
                    .build();
        }

        List<MaskType> typesToMask = request.getMaskTypes() != null && !request.getMaskTypes().isEmpty()
                ? request.getMaskTypes()
                : Arrays.asList(MaskType.values());

        String detectedLanguage = detectLanguage(text);
        log.debug("Detected language: {}", detectedLanguage);

        List<MaskedEntity> maskedEntities = new ArrayList<>();
        String maskedText = text;

        // Apply pattern-based masking
        for (MaskingPattern pattern : MaskingPattern.values()) {
            if (!typesToMask.contains(pattern.getMaskType())) {
                continue;
            }

            if (!pattern.appliesTo(detectedLanguage)) {
                continue;
            }

            maskedText = applyPattern(maskedText, pattern, maskedEntities);
        }

        // Apply AI-based name detection if NAME is in the mask types
        if (typesToMask.contains(MaskType.NAME)) {
            maskedText = maskNames(maskedText, detectedLanguage, maskedEntities);
        }

        return MaskResponse.builder()
                .maskedText(maskedText)
                .maskedEntities(maskedEntities)
                .success(true)
                .detectedLanguage(detectedLanguage)
                .build();
    }

    @Override
    public MaskResponse maskDocument(byte[] fileData, String mimeType, MaskRequest request) {
        try {
            // First extract text from document
            var processedDoc = documentProcessorService.processDocument(fileData, mimeType, null);

            if (!processedDoc.isSuccess()) {
                return MaskResponse.builder()
                        .success(false)
                        .errorMessage("Failed to process document: " + processedDoc.getErrorMessage())
                        .build();
            }

            // Mask the extracted text
            MaskResponse textMaskResponse = maskText(processedDoc.getRawText(), request);

            // For now, we return the masked text. PDF regeneration can be added later.
            return MaskResponse.builder()
                    .maskedText(textMaskResponse.getMaskedText())
                    .maskedEntities(textMaskResponse.getMaskedEntities())
                    .success(true)
                    .detectedLanguage(textMaskResponse.getDetectedLanguage())
                    .build();

        } catch (Exception e) {
            log.error("Error masking document", e);
            return MaskResponse.builder()
                    .success(false)
                    .errorMessage("Error masking document: " + e.getMessage())
                    .build();
        }
    }

    private String applyPattern(String text, MaskingPattern pattern, List<MaskedEntity> entities) {
        Matcher matcher = pattern.getPattern().matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String originalValue = matcher.group();
            entities.add(MaskedEntity.builder()
                    .type(pattern.getMaskType().name())
                    .originalValue(originalValue)
                    .maskedValue(pattern.getReplacement())
                    .startIndex(matcher.start())
                    .endIndex(matcher.end())
                    .build());

            matcher.appendReplacement(result, Matcher.quoteReplacement(pattern.getReplacement()));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private final com.app.civify.service.ai.AiService aiService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @org.springframework.beans.factory.annotation.Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private static final String NER_PROMPT = """
        Analyze the following text and identify all personal names (people's full names or partial names).
        Return ONLY a JSON array of strings containing the names found, nothing else.
        Do not include any explanation or markdown formatting.
        Example response: ["John Doe", "Jane Smith"]

        Text:
        %s
        """;

    private String maskNames(String text, String language, List<MaskedEntity> entities) {
        try {
            // Use AI to find names (NER)
            String prompt = String.format(NER_PROMPT, text);
            var aiResponse = aiService.callModel(prompt, defaultProvider, defaultModel);
            String response = aiResponse.getContent();

            // Extract actual content from Gemini response structure
            String contentText = extractContentFromGeminiResponse(response);
            String jsonArray = cleanJsonResponse(contentText);

            List<String> names = objectMapper.readValue(jsonArray, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });

            String maskedText = text;
            for (String name : names) {
                if (name == null || name.length() < 2) {
                    continue; // Skip very short matches
                }
                // Find and mask all occurrences (case insensitive)
                String replacement = language.equals("ar") ? "[الاسم_محذوف]" : "[NAME_REDACTED]";
                maskedText = maskedText.replaceAll("(?i)" + java.util.regex.Pattern.quote(name), replacement);

                entities.add(MaskedEntity.builder()
                        .type(MaskType.NAME.name())
                        .originalValue(name)
                        .maskedValue(replacement)
                        .build());
            }
            return maskedText;

        } catch (Exception e) {
            log.warn("AI Name detection failed, falling back to heuristic: {}", e.getMessage());
            // Fallback to original heuristic
            return maskNamesHeuristic(text, language, entities);
        }
    }

    /**
     * Extract the actual text content from Gemini API response structure.
     */
    private String extractContentFromGeminiResponse(String response) {
        try {
            var root = objectMapper.readTree(response);

            // Gemini response structure: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
            var candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                var content = candidates.get(0).get("content");
                if (content != null) {
                    var parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        var textNode = parts.get(0).get("text");
                        if (textNode != null) {
                            return textNode.asText();
                        }
                    }
                }
            }

            // If not in Gemini structure, return as-is
            return response;
        } catch (Exception e) {
            log.debug("Could not parse as Gemini response, using raw response: {}", e.getMessage());
            return response;
        }
    }

    private String maskNamesHeuristic(String text, String language, List<MaskedEntity> entities) {
        String maskedText = text;

        // English name patterns (after common labels)
        maskedText = maskNamePattern(maskedText,
                "(?i)(Name|Full Name|Candidate)[\\s:]+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})",
                2, "[NAME_REDACTED]", entities, MaskType.NAME);

        // Arabic name patterns
        if ("ar".equals(language)) {
            maskedText = maskNamePattern(maskedText,
                    "(الاسم|الإسم|اسم)[\\s:]+([\\u0600-\\u06FF\\s]+?)(?=\\n|$|[،,])",
                    2, "[الاسم_محذوف]", entities, MaskType.NAME);
        }

        return maskedText;
    }

    @Override
    public com.app.civify.dto.resume.ResumeData maskResumeData(com.app.civify.dto.resume.ResumeData resumeData, MaskRequest request) {
        if (resumeData == null) {
            return null;
        }

        List<MaskedEntity> entities = new ArrayList<>();
        String language = resumeData.getMetadata() != null ? resumeData.getMetadata().getLanguage() : "en";
        boolean isArabic = "ar".equals(language);

        // Mask Personal Info - directly replace known PII fields
        var info = resumeData.getPersonalInfo();
        if (info != null) {
            // Mask Name - direct replacement since we know it's a name field
            if (info.getFullName() != null && !info.getFullName().isEmpty()) {
                String originalName = info.getFullName();
                String replacement = isArabic ? "[الاسم_محذوف]" : "[NAME_REDACTED]";
                info.setFullName(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.NAME.name())
                        .originalValue(originalName)
                        .maskedValue(replacement)
                        .build());
            }

            // Mask Email - direct replacement
            if (info.getEmail() != null && !info.getEmail().isEmpty()) {
                String originalEmail = info.getEmail();
                String replacement = isArabic ? "[البريد_محذوف]" : "[EMAIL_REDACTED]";
                info.setEmail(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.EMAIL.name())
                        .originalValue(originalEmail)
                        .maskedValue(replacement)
                        .build());
            }

            // Mask Phone - direct replacement
            if (info.getPhone() != null && !info.getPhone().isEmpty()) {
                String originalPhone = info.getPhone();
                String replacement = isArabic ? "[رقم_الهاتف_محذوف]" : "[PHONE_REDACTED]";
                info.setPhone(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.PHONE.name())
                        .originalValue(originalPhone)
                        .maskedValue(replacement)
                        .build());
            }

            // Mask Address - direct replacement
            if (info.getAddress() != null && !info.getAddress().isEmpty()) {
                String originalAddress = info.getAddress();
                String replacement = isArabic ? "[العنوان_محذوف]" : "[ADDRESS_REDACTED]";
                info.setAddress(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.ADDRESS.name())
                        .originalValue(originalAddress)
                        .maskedValue(replacement)
                        .build());
            }

            // Mask LinkedIn - direct replacement
            if (info.getLinkedin() != null && !info.getLinkedin().isEmpty()) {
                String originalLinkedin = info.getLinkedin();
                String replacement = "[LINKEDIN_REDACTED]";
                info.setLinkedin(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.SOCIAL_PROFILES.name())
                        .originalValue(originalLinkedin)
                        .maskedValue(replacement)
                        .build());
            }

            // Mask GitHub
            if (info.getGithub() != null && !info.getGithub().isEmpty()) {
                String originalGithub = info.getGithub();
                String replacement = "[GITHUB_REDACTED]";
                info.setGithub(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.SOCIAL_PROFILES.name())
                        .originalValue(originalGithub)
                        .maskedValue(replacement)
                        .build());
            }

            // Mask Website
            if (info.getWebsite() != null && !info.getWebsite().isEmpty()) {
                String originalWebsite = info.getWebsite();
                String replacement = "[WEBSITE_REDACTED]";
                info.setWebsite(replacement);
                entities.add(MaskedEntity.builder()
                        .type(MaskType.SOCIAL_PROFILES.name())
                        .originalValue(originalWebsite)
                        .maskedValue(replacement)
                        .build());
            }
        }

        log.info("Masked {} PII entities in resume data", entities.size());
        return resumeData;
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String maskNamePattern(String text, String regex, int groupIndex,
            String replacement, List<MaskedEntity> entities, MaskType type) {
        try {
            var pattern = java.util.regex.Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String fullMatch = matcher.group();
                String nameValue = matcher.group(groupIndex);
                String maskedMatch = fullMatch.replace(nameValue, replacement);

                entities.add(MaskedEntity.builder()
                        .type(type.name())
                        .originalValue(nameValue.trim())
                        .maskedValue(replacement)
                        .startIndex(matcher.start(groupIndex))
                        .endIndex(matcher.end(groupIndex))
                        .build());

                matcher.appendReplacement(result, Matcher.quoteReplacement(maskedMatch));
            }
            matcher.appendTail(result);

            return result.toString();
        } catch (Exception e) {
            log.warn("Error applying name pattern: {}", e.getMessage());
            return text;
        }
    }

    private String detectLanguage(String text) {
        // Simple language detection based on character ranges
        int arabicChars = 0;
        int latinChars = 0;

        for (char c : text.toCharArray()) {
            if (c >= '\u0600' && c <= '\u06FF') {
                arabicChars++;
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                latinChars++;
            }
        }

        if (arabicChars > latinChars) {
            return "ar";
        }
        return "en";
    }
}
