package com.app.civify.service.ai.provider;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.service.ai.AiProvider;
import com.app.civify.service.ai.client.VertexAiClient;
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
public class VertexAiProvider implements AiProvider {

    private final VertexAiClient client;
    private final ObjectMapper objectMapper;

    @Value("${vertex.api.key:}")
    private String apiKey;

    public VertexAiProvider(VertexAiClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiModelResponse callModel(String prompt, String model) {
        try {
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> rolePart = Map.of("role", "user", "parts", List.of(textPart));

            Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 65536
            );

            Map<String, Object> requestMap = Map.of(
                "contents", List.of(rolePart),
                "generationConfig", generationConfig
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);
            String rawResponse = client.callModel(model, requestBody);

            if (rawResponse == null || rawResponse.isBlank()) {
                log.error("Vertex AI returned null or empty response for model={}", model);
                throw new RuntimeException("Vertex AI returned empty response");
            }

            return parseResponse(rawResponse);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build Vertex AI request: " + e.getMessage(), e);
        }
    }

    private AiModelResponse parseResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            // Check for API-level errors
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String errorMessage = error.path("message").asText("Unknown Vertex AI error");
                int errorCode = error.path("code").asInt(0);
                log.error("Vertex AI error: code={}, message={}", errorCode, errorMessage);
                throw new RuntimeException("Vertex AI error (" + errorCode + "): " + errorMessage);
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
                log.warn("Vertex AI response missing usageMetadata — token counts will be 0");
            }

            if (content.isEmpty()) {
                log.warn("Vertex AI response had no content text. Checking finish reason...");
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String finishReason = candidates.get(0).path("finishReason").asText("UNKNOWN");
                    log.warn("Vertex AI finish reason: {}", finishReason);
                    if ("SAFETY".equals(finishReason)) {
                        throw new RuntimeException("Vertex AI blocked response due to safety filters");
                    }
                }
            }

            log.debug("Vertex AI tokens: prompt={}, completion={}, total={}", promptTokens, completionTokens, totalTokens);

            return AiModelResponse.builder()
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .build();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Vertex AI response JSON: {}", e.getMessage());
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
        return "vertex";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
