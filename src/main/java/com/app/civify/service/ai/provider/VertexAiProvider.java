package com.app.civify.service.ai.provider;

import com.app.civify.service.ai.AiProvider;
import com.app.civify.service.ai.client.VertexAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
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
    public String callModel(String prompt, String model) {
        try {
            // Build Vertex AI API request structure (Same as Gemini for this endpoint)
            Map<String, Object> textPart = Map.of("text", prompt);
            
            Map<String, Object> rolePart = Map.of("role", "user", "parts", List.of(textPart));
            
            // Add generation config
            Map<String, Object> generationConfig = Map.of(
                "temperature", 0.2,
                "maxOutputTokens", 8192
            );
            
            // Vertex AI typically expects "contents" array
            Map<String, Object> requestMap = Map.of(
                "contents", List.of(rolePart),
                "generationConfig", generationConfig
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);

            return client.callModel(model, requestBody);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build Vertex AI request: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Vertex AI call failed: " + e.getMessage(), e);
        }
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
