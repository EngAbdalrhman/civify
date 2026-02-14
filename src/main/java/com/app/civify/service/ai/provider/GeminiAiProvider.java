package com.app.civify.service.ai.provider;

import com.app.civify.service.ai.AiProvider;
import com.app.civify.service.ai.client.GeminiAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAiProvider implements AiProvider {

    private final GeminiAiClient client;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    public GeminiAiProvider(GeminiAiClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public String callModel(String prompt, String model) {
        try {
            // Build Gemini API request using proper JSON serialization
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));
            
            // Add generation config for better structured output control
            Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 8192
            );
            
            Map<String, Object> requestMap = Map.of(
                "contents", List.of(parts),
                "generationConfig", generationConfig
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);

            return client.callModel(model, requestBody);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build Gemini request: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
        }
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
