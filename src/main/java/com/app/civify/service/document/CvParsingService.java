package com.app.civify.service.document;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ResumeService;
import com.app.civify.service.ai.AiService;
import com.app.civify.service.ai.TokenizerService;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Service for parsing CVs/Resumes into structured data. Integrates document
 * processing with AI-powered parsing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CvParsingService {

    private final DocumentProcessorService documentProcessorService;
    private final AiService aiService;
    private final ResumeService resumeService;
    private final SubscriptionService subscriptionService;
    private final TokenizerService tokenizerService;
    private final ObjectMapper objectMapper;

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private static final String CV_PARSE_PROMPT = """
        Extract structured data from this CV in YAML format.
        
        Structure:
        personalInfo:
          fullName: string
          email: string
          phone: string
          address: string
          city: string
          country: string
          summary: string
          jobTitle: string
          linkedin: string
          website: string
          github: string
        sections:
          - id: uuid
            title: string
            type: experience|education|skills|languages|certifications|projects|custom
            items:
              - id: uuid
                title: string
                subtitle: string
                date: string
                description: string
                location: string
                tags: [string]
        metadata:
          language: code
        
        Rules:
        1. Extract ALL content.
        2. Use original language.
        3. Valid YAML only. Do NOT return JSON. Do NOT include markdown fences.
        
        CV Text:
        %s
        """;

    /**
     * Parse a CV file and extract structured data.
     */
    public CvParseResponse parseCv(MultipartFile file, CvParseRequest request) {
        return parseCv(file, request, null);
    }

    /**
     * Parse a CV file and extract structured data with user context.
     */
    public CvParseResponse parseCv(MultipartFile file, CvParseRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Process document to extract text
            long docProcStart = System.currentTimeMillis();
            ProcessedDocument processedDoc = documentProcessorService.process(file, request.getLanguage());
            long docProcTime = System.currentTimeMillis() - docProcStart;
            log.info("Document Processing Time: {}ms", docProcTime);

            if (!processedDoc.isSuccess() || !processedDoc.hasText()) {
                return CvParseResponse.builder()
                        .success(false)
                        .message("Failed to extract text from document: "
                                + (processedDoc.getErrorMessage() != null ? processedDoc.getErrorMessage() : "No text found"))
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            log.info("Document processed, text length: {}, method: {}",
                    processedDoc.getRawText().length(), processedDoc.getProcessingMethod());

            // Step 2: Parse text with AI
            long aiStart = System.currentTimeMillis();
            String truncatedText = truncateForAi(processedDoc.getRawText());

            // Use accurate tokenizer instead of heuristic
            int estimatedTokens = tokenizerService.countTokens(truncatedText);
            log.info("Estimated tokens for CV parsing: {}", estimatedTokens);
            
            // For authenticated users, check token limit
            if (userId != null) {
                if (!subscriptionService.canUserUseTokens(userId, estimatedTokens)) {
                    return CvParseResponse.builder()
                            .success(false)
                            .message("TOKEN_LIMIT_EXCEEDED")
                            .errorCode("TOKEN_LIMIT_EXCEEDED")
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build();
                }
            } else {
                log.info("Guest user parsing CV - no token charging");
            }

            ResumeData resumeData = parseTextWithAi(truncatedText, request.isPiiOnly());
            long aiTime = System.currentTimeMillis() - aiStart;
            log.info("AI Parsing Time: {}ms", aiTime);

            if (resumeData == null) {
                return CvParseResponse.builder()
                        .success(false)
                        .message("Failed to parse CV content with AI")
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Step 3: Optionally save to database (only for authenticated users)
            String resumeId = null;
            if (request.isSaveToDatabase()) {
                if (userId != null) {
                    String title = request.getResumeTitle() != null ? request.getResumeTitle() : "Parsed CV";
                    var savedResume = resumeService.createResume(userId, title, resumeData);
                    resumeId = savedResume.getId();
                    log.info("Resume saved with ID: {}", resumeId);
                } else {
                    log.warn("Guest user attempted to save CV - operation skipped");
                }
            }

            // Step 4: Charge tokens after successful parse
            if (userId != null) {
                subscriptionService.chargeTokens(userId, estimatedTokens);
            }

            return CvParseResponse.builder()
                    .success(true)
                    .resumeData(resumeData)
                    .resumeId(resumeId)
                    .confidence(processedDoc.getConfidence())
                    .processingMethod(processedDoc.getProcessingMethod())
                    .detectedLanguage(processedDoc.getPrimaryLanguage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing CV", e);
            return CvParseResponse.builder()
                    .success(false)
                    .message("Error parsing CV: " + e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Parse extracted text using AI to produce structured ResumeData.
     */
    private static final String PII_PARSE_PROMPT = """
        Extract structured data from this CV in YAML format.
        
        Structure:
        personalInfo:
          fullName: string
          email: string
          phone: string
          address: string
          city: string
          country: string
          summary: string
          jobTitle: string
          linkedin: string
          website: string
          github: string
        sections:
          - id: uuid
            title: string
            type: experience|education|skills|languages|certifications|projects|custom
            items:
              - id: uuid
                title: string
                subtitle: string
                date: string
                description: string
                location: string
                tags: [string]
        metadata:
          language: code
        
        Rules:
        1. Extract ALL content.
        2. Use original language.
        3. Valid YAML only.
        
        CV Text:
        %s
        """;

    /**
     * Parse extracted text using AI to produce structured ResumeData.
     */
    private ResumeData parseTextWithAi(String text, boolean piiOnly) {
        try {
            String promptTemplate = piiOnly ? PII_PARSE_PROMPT : CV_PARSE_PROMPT;
            String prompt = String.format(promptTemplate, wrapUserText(text));

            String response = aiService.callModel(prompt, defaultProvider, defaultModel);

            if (response == null || response.isEmpty()) {
                log.error("Empty response from AI");
                return null;
            }

            String parsedContent = tryParseContent(response, piiOnly);

            if (parsedContent == null) {
                log.warn("LLM output invalid, retrying with corrective prompt");
                String repairPrompt = prompt
                        + "\n\nOutput was not valid "
                        + (piiOnly ? "YAML" : "JSON")
                        + ". Return STRICT "
                        + (piiOnly ? "YAML" : "JSON")
                        + " only, no prose, no markdown fences.";
                String retryResponse = aiService.callModel(repairPrompt, defaultProvider, defaultModel);
                parsedContent = tryParseContent(retryResponse, piiOnly);
            }

            if (parsedContent == null) {
                return null;
            }

            // Parse as YAML only (no JSON fallback)
            try {
                ObjectMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
                yamlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return yamlMapper.readValue(parsedContent, ResumeData.class);
            } catch (Exception yamlEx) {
                log.error("❌ YAML parsing failed: {}", yamlEx.getMessage());
                log.error("Content that failed: {}", parsedContent);
                return null;
            }

        } catch (Exception e) {
            log.error("Error parsing CV with AI: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Remove markdown code blocks from JSON/YAML response. 
     * AI models sometimes wrap content in ```json ... ``` or ```yaml ... ``` blocks.
     */
    private String cleanMarkdownJson(String text) {
        if (text == null) {
            return null;
        }

        String cleaned = text.trim();

        // Remove ```json, ```yaml, or ``` prefix
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```yaml")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        // Sometimes the model just outputs "yaml" or "json" at the start without backticks
        if (cleaned.startsWith("yaml\n") || cleaned.startsWith("yaml ")) {
             cleaned = cleaned.substring(5);
        }

        // Remove trailing ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * Attempt to parse the AI output as YAML; return null on failure (caller may retry).
     * JSON fallback removed - strict YAML only per implementation plan.
     */
    private String tryParseContent(String response, boolean piiOnly) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String contentText = extractContentFromGeminiResponse(response);
        contentText = cleanMarkdownJson(contentText);
        log.debug("AI Response Content (Cleaned): {}", contentText);

        try {
            ObjectMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
            yamlMapper.readTree(contentText); // Validate YAML structure
            return contentText;
        } catch (Exception e) {
            log.warn("❌ Failed to parse AI output as YAML: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Wrap user-provided text to reduce prompt-injection risk.
     */
    private String wrapUserText(String text) {
        if (text == null) {
            return "";
        }
        return """
                <<<USER_INPUT_START>>>
                %s
                <<<USER_INPUT_END>>>
                """.formatted(text);
    }

    @Value("${cv.parsing.max-chars:16000}")
    private int maxChars;

    /**
     * Cap input size to control token usage (~4k tokens heuristic at 4 chars/token).
     */
    private String truncateForAi(String rawText) {
        if (rawText == null) {
            return "";
        }
        if (rawText.length() <= maxChars) {
            return rawText;
        }
        log.warn("Truncating CV text from {} to {} characters to control token usage", rawText.length(), maxChars);
        return rawText.substring(0, maxChars);
    }

    /**
     * Hash file content for cache key generation.
     * Used for CV parsing caching based on file content.
     */
    @Cacheable(value = "cv-parsing", key = "#fileHash")
    public ResumeData parseCvCached(String fileHash, String text, boolean piiOnly) {
        return parseTextWithAi(text, piiOnly);
    }
    
    private String generateFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Failed to generate file hash", e);
            return String.valueOf(System.currentTimeMillis()); // Fallback
        }
    }

    private String extractContentFromGeminiResponse(String response) {
        try {
            var root = objectMapper.readTree(response);
            var candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                var content = candidates.get(0).path("content");
                var parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            return response; // Fallback
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response structure: {}", e.getMessage());
            return response;
        }
    }
}
