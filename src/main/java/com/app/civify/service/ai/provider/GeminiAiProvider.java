package com.app.civify.service.ai.provider;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.service.ai.AiProvider;
import com.app.civify.service.ai.client.GeminiAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiAiProvider implements AiProvider {

    private final GeminiAiClient client;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${ai.model.maxOutputTokens:8192}")
    private int maxOutputTokens;

    @Value("${ai.model.thinkingBudget:1024}")
    private int thinkingBudget;

    public GeminiAiProvider(GeminiAiClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiModelResponse callModel(String prompt, String model) {
        try {
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));

            Map<String, Object> generationConfig = new java.util.HashMap<>(Map.of(
                "temperature", 0.2,
                "maxOutputTokens", maxOutputTokens
            ));

            if (thinkingBudget > 0) {
                generationConfig.put("thinkingConfig", Map.of("thinkingBudget", thinkingBudget));
            }

            Map<String, Object> requestMap = Map.of(
                "contents", List.of(parts),
                "generationConfig", generationConfig
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);
            String rawResponse = client.callModel(model, requestBody);

            if (rawResponse == null || rawResponse.isBlank()) {
                log.error("Gemini API returned null or empty response for model={}", model);
                throw new RuntimeException("Gemini API returned empty response");
            }

            return parseResponse(rawResponse);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build Gemini request: " + e.getMessage(), e);
        }
    }

    private AiModelResponse parseResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            // Check for API-level errors
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String errorMessage = error.path("message").asText("Unknown Gemini API error");
                int errorCode = error.path("code").asInt(0);
                log.error("Gemini API error: code={}, message={}", errorCode, errorMessage);
                throw new RuntimeException("Gemini API error (" + errorCode + "): " + errorMessage);
            }

            // Extract content
            String content = extractContent(root);

            // Extract token usage
            int promptTokens = 0;
            int completionTokens = 0;
            int totalTokens = 0;

            JsonNode usageMetadata = root.path("usageMetadata");
            if (!usageMetadata.isMissingNode()) {
                promptTokens = usageMetadata.path("promptTokenCount").asInt(0);
                completionTokens = usageMetadata.path("candidatesTokenCount").asInt(0);
                totalTokens = usageMetadata.path("totalTokenCount").asInt(0);
            } else {
                log.warn("Gemini response missing usageMetadata — token counts will be 0");
            }

            // Validate: if content is empty but no error, log warning
            if (content.isEmpty()) {
                log.warn("Gemini response had no content text. Checking finish reason...");
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String finishReason = candidates.get(0).path("finishReason").asText("UNKNOWN");
                    log.warn("Gemini finish reason: {}", finishReason);
                    if ("SAFETY".equals(finishReason)) {
                        throw new RuntimeException("Gemini blocked response due to safety filters");
                    }
                }
            }

            log.debug("Gemini tokens: prompt={}, completion={}, total={}", promptTokens, completionTokens, totalTokens);

            return AiModelResponse.builder()
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .build();

        } catch (RuntimeException e) {
            // Re-throw RuntimeExceptions (API errors, safety blocks) as-is
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response JSON: {}", e.getMessage());
            // Return fallback with raw content instead of crashing
            return AiModelResponse.builder()
                    .content(rawResponse)
                    .promptTokens(0)
                    .completionTokens(0)
                    .totalTokens(0)
                    .build();
        }
    }

    private String extractContent(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    String text = part.path("text").asText("");
                    if (!text.isEmpty()) {
                        sb.append(text);
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
