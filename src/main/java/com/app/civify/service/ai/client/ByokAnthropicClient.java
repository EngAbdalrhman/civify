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
public class ByokAnthropicClient {

    private static final String BASE_URL = "https://api.anthropic.com";

    private final ObjectMapper objectMapper;

    public AiModelResponse complete(String apiKey, String model, String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 32,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String jsonBody = objectMapper.writeValueAsString(body);

            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", "2023-06-01")
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String raw = client.post()
                    .uri("/v1/messages")
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                throw new RuntimeException(error.path("message").asText("Anthropic API error"));
            }
            StringBuilder content = new StringBuilder();
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    content.append(block.path("text").asText(""));
                }
            }
            JsonNode usage = root.path("usage");
            int input = usage.path("input_tokens").asInt(0);
            int output = usage.path("output_tokens").asInt(0);
            return AiModelResponse.builder()
                    .content(content.toString())
                    .promptTokens(input)
                    .completionTokens(output)
                    .totalTokens(input + output)
                    .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Anthropic request failed: " + e.getMessage(), e);
        }
    }
}
