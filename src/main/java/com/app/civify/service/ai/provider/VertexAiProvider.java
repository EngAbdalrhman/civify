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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${vertex.api.key:}")
    private String apiKey;

    public VertexAiProvider(VertexAiClient client) {
        this.client = client;
    }

    @Override
    public String callModel(String prompt, String model) {
        try {
            // Build Vertex AI API request structure (Same as Gemini for this endpoint)
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));
            Map<String, Object> rolePart = Map.of("role", "user", "parts", List.of(textPart));
            
            // Vertex AI typically expects "contents" array
            Map<String, Object> requestMap = Map.of("contents", List.of(rolePart));

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
