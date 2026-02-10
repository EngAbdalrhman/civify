package com.app.civify.service.document.ocr;

import com.app.civify.dto.document.OcrResult;
import com.app.civify.service.ai.GeminiAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * OCR service using Google Gemini Vision API.
 * Primary OCR provider with excellent Arabic support.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class GeminiVisionOcrService implements OcrService {
    
    private final GeminiAiClient geminiAiClient;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.gemini.vision.model:gemini-2.5-flash}")
    private String visionModel;
    
    @Value("${ai.gemini.enabled:true}")
    private boolean enabled;
    
    private static final String OCR_PROMPT_TEMPLATE = """
            Extract ALL text from this image exactly as it appears. This is a CV/resume document.
            
            Instructions:
            1. Preserve the exact text, including any Arabic (العربية) or other languages
            2. Maintain the general structure and section ordering
            3. Do NOT translate or modify any text
            4. If text is unclear, indicate with [unclear]
            5. Extract text from tables maintaining row/column structure
            
            %s
            
            Return ONLY the extracted text, nothing else.
            """;
    
    @Override
    public OcrResult extractText(byte[] imageData, String mimeType) {
        return extractText(imageData, mimeType, null);
    }
    
    @Override
    public OcrResult extractText(byte[] imageData, String mimeType, String languageHint) {
        long startTime = System.currentTimeMillis();
        
        if (!isAvailable()) {
            return OcrResult.error("Gemini Vision service is not available");
        }
        
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String languageInstruction = buildLanguageInstruction(languageHint);
            String prompt = String.format(OCR_PROMPT_TEMPLATE, languageInstruction);
            
            // Build Gemini Vision API request with image
            String requestBody = buildVisionRequest(prompt, base64Image, mimeType);
            
            String response = geminiAiClient.callModel(visionModel, requestBody);
            String extractedText = parseGeminiResponse(response);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Gemini Vision OCR completed in {}ms, extracted {} characters", 
                    processingTime, extractedText.length());
            
            return OcrResult.builder()
                    .text(extractedText)
                    .confidence(0.95) // Gemini typically has high accuracy
                    .success(true)
                    .ocrProvider("GEMINI_VISION")
                    .processingTimeMs(processingTime)
                    .build();
                    
        } catch (Exception e) {
            log.error("Gemini Vision OCR failed: {}", e.getMessage(), e);
            return OcrResult.builder()
                    .text("")
                    .confidence(0.0)
                    .success(false)
                    .errorMessage("Gemini Vision OCR failed: " + e.getMessage())
                    .ocrProvider("GEMINI_VISION")
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && geminiAiClient != null;
    }
    
    @Override
    public String getProviderName() {
        return "GEMINI_VISION";
    }
    
    @Override
    public int getPriority() {
        return 1; // Highest priority
    }
    
    private String buildLanguageInstruction(String languageHint) {
        if (languageHint == null || languageHint.isEmpty()) {
            return "The document may contain Arabic, English, or mixed content.";
        }
        
        return switch (languageHint.toLowerCase()) {
            case "ar", "arabic" -> "This document is primarily in Arabic (العربية). Pay special attention to RTL text and Arabic numerals.";
            case "en", "english" -> "This document is primarily in English.";
            default -> "This document may be in " + languageHint + " or mixed with other languages.";
        };
    }
    
    private String buildVisionRequest(String prompt, String base64Image, String mimeType) {
        // Normalize mime type
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "image/png";
        }
        
        return String.format("""
                {
                    "contents": [{
                        "parts": [
                            {"text": "%s"},
                            {
                                "inline_data": {
                                    "mime_type": "%s",
                                    "data": "%s"
                                }
                            }
                        ]
                    }]
                }
                """, 
                escapeJson(prompt), 
                mimeType, 
                base64Image);
    }
    
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private String parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText("");
                }
            }
            
            return response; // Return raw response if parsing fails
        } catch (Exception e) {
            log.warn("Could not parse Gemini response: {}", e.getMessage());
            return response;
        }
    }
}

