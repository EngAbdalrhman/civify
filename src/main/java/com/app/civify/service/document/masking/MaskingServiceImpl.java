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
        Analyze the following text and identify all personal names.
        Return ONLY a JSON array of strings containing the names found.
        Example: ["John Doe", "Jane Smith"]
        
        Text:
        %s
        """;

    private String maskNames(String text, String language, List<MaskedEntity> entities) {
        try {
            // Use AI to find names (NER)
            String prompt = String.format(NER_PROMPT, text);
            String response = aiService.callModel(prompt, defaultProvider, defaultModel);
            
            String jsonArray = cleanJsonResponse(response);
            List<String> names = objectMapper.readValue(jsonArray, new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
            
            String maskedText = text;
            for (String name : names) {
                if (name.length() < 3) continue; // Skip very short matches
                
                // Find and mask all occurrences
                maskedText = maskNamePattern(maskedText, 
                    java.util.regex.Pattern.quote(name), 
                    0, 
                    language.equals("ar") ? "[الاسم_محذوف]" : "[NAME_REDACTED]", 
                    entities, 
                    MaskType.NAME);
            }
            return maskedText;
            
        } catch (Exception e) {
            log.warn("AI Name detection failed, falling back to heuristic: {}", e.getMessage());
            // Fallback to original heuristic
            return maskNamesHeuristic(text, language, entities);
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
        if (resumeData == null) return null;
        
        List<MaskedEntity> entities = new ArrayList<>();
        String language = resumeData.getMetadata() != null ? resumeData.getMetadata().getLanguage() : "en";
        
        // Mask Personal Info
        var info = resumeData.getPersonalInfo();
        if (info != null) {
            // Mask Name
            if (info.getFullName() != null) {
                info.setFullName(maskNames(info.getFullName(), language, entities));
            }
            
            // Mask Email
            if (info.getEmail() != null) {
                info.setEmail(applyPattern(info.getEmail(), MaskingPattern.EMAIL_ENGLISH, entities));
            }
            
            // Mask Phone
            if (info.getPhone() != null) {
                String maskedPhone = applyPattern(info.getPhone(), MaskingPattern.PHONE_INTERNATIONAL, entities);
                if (maskedPhone.equals(info.getPhone()) && "ar".equals(language)) {
                    maskedPhone = applyPattern(info.getPhone(), MaskingPattern.PHONE_ARABIC, entities);
                }
                info.setPhone(maskedPhone);
            }
            
            // Mask Address
            if (info.getAddress() != null) {
                String maskedAddress = applyPattern(info.getAddress(), MaskingPattern.ADDRESS_ENGLISH, entities);
                if (maskedAddress.equals(info.getAddress()) && "ar".equals(language)) {
                    maskedAddress = applyPattern(info.getAddress(), MaskingPattern.ADDRESS_ARABIC, entities);
                }
                info.setAddress(maskedAddress);
            }
            
            // Mask Social Links
            if (info.getLinkedin() != null) {
                info.setLinkedin(applyPattern(info.getLinkedin(), MaskingPattern.LINKEDIN, entities));
            }
        }
        
        return resumeData;
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
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
