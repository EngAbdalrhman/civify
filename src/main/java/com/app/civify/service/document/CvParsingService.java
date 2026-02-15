package com.app.civify.service.document;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.exception.BadRequestException;
import com.app.civify.service.ai.AiService;
import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.service.ai.TokenizerService;
import com.app.civify.service.ai.JsonSanitizer;
import com.app.civify.service.subscription.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



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
    private final SubscriptionService subscriptionService;
    private final TokenizerService tokenizerService;
    private final ObjectMapper objectMapper;
    private final BucketService bucketService;
    private final JsonSanitizer jsonSanitizer;

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
                tags: [string] # Flat list of strings only
        metadata:
          language: code
        
        Rules:
        1. Extract ALL content.
        2. Use original language.
        3. Valid YAML only. Do NOT return JSON. Do NOT include markdown fences.
        4. CRITICAL: ALWAYS wrap key string values in double quotes if they contain special characters (like :) or just always quote them to be safe. E.g. title: "Software Engineer".
        5. Escape special characters inside strings.
        
        CV Text:
        %s
        """;

    /**
     * Parse a CV file and extract structured data with user context.
     */
    public CvParseResponse parseCv(MultipartFile file, CvParseRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        if (userId == null)
            throw new BadRequestException("user.notfound");

        try {
            // Step 0: Store CV in server folder
            bucketService.storeCv(file, userId);

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
            
            if (!subscriptionService.canUserUseTokens(userId, estimatedTokens)) {
                return CvParseResponse.builder()
                        .success(false)
                        .message("TOKEN_LIMIT_EXCEEDED")
                        .errorCode("TOKEN_LIMIT_EXCEEDED")
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            ParsedCvResult parseResult = parseTextWithAi(truncatedText, request);
            long aiTime = System.currentTimeMillis() - aiStart;
            log.info("AI Parsing Time: {}ms", aiTime);

            if (parseResult == null || parseResult.resumeData == null) {
                return CvParseResponse.builder()
                        .success(false)
                        .message("Failed to parse CV content with AI")
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            ResumeData resumeData = parseResult.resumeData;
            int actualTokens = parseResult.tokensUsed;



            // Step 4: Charge tokens after successful parse
            if (userId != null) {
                // Charge ACTUAL tokens instead of estimated
                subscriptionService.chargeTokens(userId, actualTokens, TokenUsageType.CV_PARSE, "CV Upload Parsing");
                log.info("Charged {} tokens (Estimated: {}) to user {}", actualTokens, estimatedTokens, userId);
            }

            return CvParseResponse.builder()
                    .success(true)
                    .resumeData(resumeData)
                    .confidence(processedDoc.getConfidence())
                    .processingMethod(processedDoc.getProcessingMethod())
                    .detectedLanguage(processedDoc.getPrimaryLanguage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .tokensUsed(actualTokens)
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
                tags: [string] # Flat list of strings only
        metadata:
          language: code
        
        Rules:
        1. Extract ALL content.
        2. Use original language.
        3. Valid YAML only.
        4. CRITICAL: ALWAYS wrap string values in double quotes.
        5. Escape special characters inside strings.
        
        CV Text:
        %s
        """;

    /**
     * Result holder for parsed CV data and token usage.
     */
    private record ParsedCvResult(ResumeData resumeData, int tokensUsed) {}



    private ParsedCvResult parseTextWithAi(String text, CvParseRequest request) {
        try {
            String promptTemplate;
            boolean useCustomTemplate = request.getUserTemplate() != null && !request.getUserTemplate().isBlank();

            if (useCustomTemplate) {
                promptTemplate = """
                    Extract data from this CV based on the following user-defined structure/template on %s (JSON or YAML).
                    
                    User Template:
                    %s
                    
                    Rules:
                    1. Extract content from CV and map it to the structure above.
                    2. Use original language from the CV.
                    3. Output MUST be valid %s (matching the template format).
                    4. Do not include markdown fences/blocks if possible, or just standard ```json / ```yaml.
                    
                    CV Text:
                    %%s
                    """.formatted(
                        "YAML", // Hinting YAML as priority if ambiguous, but really we follow user template
                         request.getUserTemplate(),
                        "YAML"
                    );
                // Dynamic prompt construction can be tricky. Let's make it simpler and robust.
                // If user provides template, we use it as the "Structure" part of our standard prompt or just direct instruction.
                
                // Better approach:
                 promptTemplate = """
                    Extract structured data from this CV according to the provided User Template.
                    
                    User Template:
                    %s
                    
                    Rules:
                    1. Extract ALL relevant content matching the template keys.
                    2. Maintain the structure of the User Template.
                    3. Return valid JSON or YAML depending on what the template looks like (default to YAML if unsure).
                    4. Strictly NO prose. Only the data.
                    5. CRITICAL: If returning YAML, double-quote all string values.
                    
                    CV Text:
                    %%s
                    """.formatted(request.getUserTemplate());
            } else {
                promptTemplate = request.isPiiOnly() ? PII_PARSE_PROMPT : CV_PARSE_PROMPT;
            }

            String prompt = String.format(promptTemplate, wrapUserText(text));

            AiModelResponse response = aiService.callModel(prompt, defaultProvider, defaultModel);

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.error("Empty response from AI");
                return null;
            }

            String parsedContent = tryParseContent(response.getContent(), request.isPiiOnly());
            int totalTokens = response.getTotalTokens();

            if (parsedContent == null) {
                log.warn("LLM output invalid, retrying with corrective prompt");
                String repairPrompt = prompt
                        + "\n\nOutput was not valid "
                        + (request.isPiiOnly() ? "YAML" : "structured data")
                        + ". Return STRICT structured data only, no prose, no markdown fences.";
                AiModelResponse retryResponse = aiService.callModel(repairPrompt, defaultProvider, defaultModel);
                parsedContent = tryParseContent(retryResponse.getContent(), request.isPiiOnly());
                totalTokens += retryResponse.getTotalTokens(); // Add corrective tokens
            }

            if (parsedContent == null) {
                return null;
            }

            // Parse as YAML or JSON
            try {
                ObjectMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
                yamlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                ResumeData data = yamlMapper.readValue(parsedContent, ResumeData.class);
                
                if (jsonSanitizer.isTruncated(data)) {
                    log.warn("Parsed ResumeData appears truncated (missing critical fields)");
                }
                return new ParsedCvResult(data, totalTokens);
            } catch (Exception yamlEx) {
                // Fallback to JSON
                try {
                    ResumeData data = objectMapper.readValue(parsedContent, ResumeData.class);
                    if (jsonSanitizer.isTruncated(data)) {
                         log.warn("Parsed ResumeData appears truncated (missing critical fields)");
                    }
                    return new ParsedCvResult(data, totalTokens);
                } catch (Exception jsonEx) {
                    
                    log.error("❌ Parsing failed for both YAML and JSON.");
                    log.error("YAML Error: {}", yamlEx.getMessage());
                    log.error("JSON Error: {}", jsonEx.getMessage());
                    log.error("Content: {}", parsedContent);
                    return null;
                }
            }

        } catch (Exception e) {
            log.error("Error parsing CV with AI: {}", e.getMessage());
            return null;
        }
    }

    /**

    /**
     * Attempt to parse the AI output as YAML; return null on failure (caller may retry).
     * JSON fallback removed - strict YAML only per implementation plan.
     */
    private String tryParseContent(String response, boolean piiOnly) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String contentText = jsonSanitizer.cleanAiResponse(response);
        log.debug("AI Response Content (Cleaned): {}", contentText);

        try {
            ObjectMapper yamlMapper = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper();
            yamlMapper.readTree(contentText); // Validate YAML structure
            return contentText;
        } catch (Exception e) {
            // Try JSON
            try {
                objectMapper.readTree(contentText);
                log.debug("Fallback: Parsed valid JSON");
                return contentText;
            } catch (Exception jsonEx) {
                log.warn("❌ Failed to parse AI output as YAML or JSON: {}", e.getMessage());
                return null;
            }
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
        ParsedCvResult result = parseTextWithAi(text, CvParseRequest.builder().piiOnly(piiOnly).build());
        return result != null ? result.resumeData : null;
    }
    
    
}
