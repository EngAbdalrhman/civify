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
    public com.app.civify.dto.resume.ResumeData maskResumeData(com.app.civify.dto.resume.ResumeData source, MaskRequest request) {
        if (source == null) return null;
        // Preserve the original for auditing and callers that retain parsed data.
        var resumeData = objectMapper.convertValue(source, com.app.civify.dto.resume.ResumeData.class);
        if (request == null) request = MaskRequest.builder().build();
        final MaskRequest policy = request;
        List<MaskedEntity> entities = new ArrayList<>();
        var info = resumeData.getPersonalInfo();
        if (info != null) {
            info.setFullName(redactField(info.getFullName(), MaskType.NAME, policy, entities));
            info.setEmail(redactField(info.getEmail(), MaskType.EMAIL, policy, entities));
            info.setPhone(redactField(info.getPhone(), MaskType.PHONE, policy, entities));
            info.setAddress(redactField(info.getAddress(), MaskType.ADDRESS, policy, entities));
            info.setLinkedin(redactField(info.getLinkedin(), MaskType.SOCIAL_PROFILES, policy, entities));
            info.setGithub(redactField(info.getGithub(), MaskType.SOCIAL_PROFILES, policy, entities));
            info.setWebsite(redactField(info.getWebsite(), MaskType.SOCIAL_PROFILES, policy, entities));
            // Do not globally replace a city/country that can also identify an employer/project.
            info.setCity(redactField(info.getCity(), MaskType.ADDRESS, policy, new ArrayList<>()));
            info.setCountry(redactField(info.getCountry(), MaskType.ADDRESS, policy, new ArrayList<>()));
        }
        if (resumeData.getSections() != null) {
            for (var section : resumeData.getSections()) {
                if (section.getItems() == null) continue;
                for (var item : section.getItems()) {
                    MaskType type = personalLabelType(item.getTitle());
                    if (type == null || !policy.shouldMask(type)) continue;
                    item.setSubtitle(redactField(item.getSubtitle(), type, policy, entities));
                    item.setDescription(redactField(item.getDescription(), type, policy, entities));
                    item.setDate(redactField(item.getDate(), type, policy, entities));
                    item.setLocation(redactField(item.getLocation(), type, policy, entities));
                    if (item.getTags() != null) item.setTags(item.getTags().stream()
                            .map(value -> redactField(value, type, policy, entities)).toList());
                }
            }
            // Known personal details can be repeated outside the contact header.
            for (var section : resumeData.getSections()) {
                section.setTitle(redactKnownValues(section.getTitle(), entities));
                if (section.getItems() == null) continue;
                for (var item : section.getItems()) {
                    item.setTitle(redactKnownValues(item.getTitle(), entities));
                    item.setSubtitle(redactKnownValues(item.getSubtitle(), entities));
                    item.setDescription(redactKnownValues(item.getDescription(), entities));
                    item.setDate(redactKnownValues(item.getDate(), entities));
                    item.setLocation(redactKnownValues(item.getLocation(), entities));
                    if (item.getTags() != null) item.setTags(item.getTags().stream()
                            .map(value -> redactKnownValues(value, entities)).toList());
                }
            }
        }
        if (info != null) {
            info.setSummary(redactKnownValues(info.getSummary(), entities));
            info.setJobTitle(redactKnownValues(info.getJobTitle(), entities));
        }
        log.info("Masked {} known PII values in resume data", entities.size());
        return resumeData;
    }

    private String redactField(String value, MaskType type, MaskRequest policy, List<MaskedEntity> entities) {
        if (value == null || value.isBlank() || !policy.shouldMask(type)) return value;
        String replacement = "[" + (type == MaskType.ALL ? "PERSONAL_DETAIL" : type.name()) + "_REDACTED]";
        // Generic demographic words (e.g. "Single") may occur in unrelated job skills.
        if (type != MaskType.ALL) entities.add(MaskedEntity.builder().type(type.name()).originalValue(value).maskedValue(replacement).build());
        return replacement;
    }

    private MaskType personalLabelType(String label) {
        if (label == null) return null;
        String key = label.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
        return switch (key) {
            case "name", "fullname", "الاسم", "الإسم" -> MaskType.NAME;
            case "email", "emailaddress", "البريدالإلكتروني", "البريدالالكتروني" -> MaskType.EMAIL;
            case "phone", "phonenumber", "mobile", "mobilenumber", "الهاتف", "رقمالهاتف" -> MaskType.PHONE;
            case "address", "residence", "city", "country", "العنوان", "الإقامة", "المدينة" -> MaskType.ADDRESS;
            case "dob", "dateofbirth", "birthdate", "تاريخالميلاد", "تاريخميلاد" -> MaskType.DATE_OF_BIRTH;
            case "nationalid", "passport", "passportnumber", "رقمالهوية", "الرقمالقومي" -> MaskType.NATIONAL_ID;
            case "linkedin", "github", "website" -> MaskType.SOCIAL_PROFILES;
            case "gender", "sex", "maritalstatus", "nationality", "militarystatus", "الجنس", "الجنسية", "الحالةالاجتماعية", "الموقفمنالتجنيد" -> MaskType.ALL;
            default -> null;
        };
    }

    private String redactKnownValues(String text, List<MaskedEntity> entities) {
        if (text == null || text.isBlank()) return text;
        String result = text;
        for (MaskedEntity entity : entities) {
            // Boundaries avoid replacing a short name inside an unrelated company name.
            var pattern = java.util.regex.Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])"
                    + java.util.regex.Pattern.quote(entity.getOriginalValue()) + "(?![\\p{L}\\p{N}])");
            result = pattern.matcher(result).replaceAll(Matcher.quoteReplacement(entity.getMaskedValue()));
        }
        return result;
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
