package com.app.civify.service.document;

import com.app.civify.dto.document.CvParseRequest;
import com.app.civify.dto.document.CvParseResponse;
import com.app.civify.dto.document.ProcessedDocument;
import com.app.civify.dto.resume.ResumeData;
import com.app.civify.service.ResumeService;
import com.app.civify.service.ai.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final ResumeService resumeService;
    private final ObjectMapper objectMapper;

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    private static final String CV_PARSE_PROMPT = """
        You are an expert CV/Resume parser. Parse the following CV text and extract structured information.
        
        Return a JSON object with this exact structure:
        {
            "personalInfo": {
                "fullName": "string or null",
                "email": "string or null",
                "phone": "string or null",
                "address": "string or null",
                "city": "string or null",
                "country": "string or null",
                "summary": "string or null",
                "jobTitle": "string or null",
                "linkedin": "string or null",
                "website": "string or null",
                "github": "string or null"
            },
            "sections": [
                {
                    "id": "unique-id",
                    "title": "Section Title",
                    "type": "experience|education|skills|languages|certifications|projects|custom",
                    "items": [
                        {
                            "id": "unique-id",
                            "title": "Company/School Name",
                            "subtitle": "Job Title/Degree",
                            "date": "Date Range",
                            "description": "Description text",
                            "location": "Location",
                            "tags": ["skill1", "skill2"],
                            "isVisible": true
                        }
                    ],
                    "isVisible": true,
                    "order": 1
                }
            ],
            "metadata": {
                "language": "detected language code (en, ar, etc.)"
            }
        }
        
        Important:
        - Extract ALL information from the CV
        - Preserve the original language of the content
        - For Arabic CVs, keep Arabic text as-is
        - Generate unique IDs for sections and items
        - Order sections logically (experience, education, skills, etc.)
        - Return ONLY valid JSON, no markdown or explanations
        
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
            ProcessedDocument processedDoc = documentProcessorService.process(file, request.getLanguage());

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
            ResumeData resumeData = parseTextWithAi(processedDoc.getRawText());

            if (resumeData == null) {
                return CvParseResponse.builder()
                        .success(false)
                        .message("Failed to parse CV content with AI")
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Step 3: Optionally save to database
            String resumeId = null;
            if (request.isSaveToDatabase() && userId != null) {
                String title = request.getResumeTitle() != null ? request.getResumeTitle() : "Parsed CV";
                var savedResume = resumeService.createResume(userId, title, resumeData);
                resumeId = savedResume.getId();
                log.info("Resume saved with ID: {}", resumeId);
            }

            return CvParseResponse.builder()
                    .success(true)
                    .resumeData(resumeData)
                    .resumeId(resumeId)
                    .rawText(processedDoc.getRawText())
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
    private ResumeData parseTextWithAi(String text) {
        try {
            String prompt = String.format(CV_PARSE_PROMPT, text);
            
            // Build JSON request with generation config for JSON mode
            String requestBody = buildGeminiRequest(prompt);
            
            String response = aiService.callModel(requestBody, defaultProvider, defaultModel);

            if (response == null || response.isEmpty()) {
                log.error("Empty response from AI");
                return null;
            }

            // Parse Gemini response structure to get the actual content
            String contentText = extractContentFromGeminiResponse(response);
            
            // Parse the content JSON to ResumeData
            return objectMapper.readValue(contentText, ResumeData.class);

        } catch (Exception e) {
            log.error("Error parsing CV with AI: {}", e.getMessage());
            return null;
        }
    }

    private String buildGeminiRequest(String prompt) {
        // Escape special characters in prompt for JSON
        String escapedPrompt = prompt.replace("\"", "\\\"")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r")
                                   .replace("\t", "\\t");

        return String.format("""
            {
                "contents": [{
                    "parts": [{"text": "%s"}]
                }],
                "generationConfig": {
                    "responseMimeType": "application/json"
                }
            }
            """, escapedPrompt);
    }

    private String extractContentFromGeminiResponse(String response) {
        try {
            var root = objectMapper.readTree(response);
            var candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                var content = candidates.get(0).path("content");
                var parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
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
