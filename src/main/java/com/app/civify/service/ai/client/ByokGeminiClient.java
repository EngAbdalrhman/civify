package com.app.civify.service.ai.client;

import com.app.civify.dto.ai.AiModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ByokGeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private final ObjectMapper objectMapper;
    private final ByokRestClientFactory restClientFactory;

    public AiModelResponse complete(String apiKey, String model, String prompt) {
        try {
            Map<String, Object> requestMap = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 32)
            );
            String jsonBody = objectMapper.writeValueAsString(requestMap);

            RestClient client = restClientFactory.forBaseUrl(BASE_URL);
            String raw = client.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                throw new RuntimeException(error.path("message").asText("Gemini API error"));
            }
            String content = "";
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    content = parts.get(0).path("text").asText("");
                }
            }
            JsonNode usage = root.path("usageMetadata");
            int promptTokens = usage.path("promptTokenCount").asInt(0);
            int completionTokens = usage.path("candidatesTokenCount").asInt(0);
            return AiModelResponse.builder()
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(usage.path("totalTokenCount").asInt(promptTokens + completionTokens))
                    .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gemini BYOK request failed: " + e.getMessage(), e);
        }
    }
}
