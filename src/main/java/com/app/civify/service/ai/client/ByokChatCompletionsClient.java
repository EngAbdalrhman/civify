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

/**
 * OpenAI-compatible chat completions (OpenAI, OpenRouter).
 */
@Component
@RequiredArgsConstructor
public class ByokChatCompletionsClient {

    private final ObjectMapper objectMapper;
    private final ByokRestClientFactory restClientFactory;

    public AiModelResponse complete(String baseUrl, String apiKey, String model, String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 32,
                    "temperature", 0.0
            );
            String jsonBody = objectMapper.writeValueAsString(body);

            RestClient client = restClientFactory.forBaseUrl(baseUrl);

            String raw = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            return parseOpenAiCompatible(raw);
        } catch (Exception e) {
            throw new RuntimeException("Chat completions request failed: " + e.getMessage(), e);
        }
    }

    private AiModelResponse parseOpenAiCompatible(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode error = root.path("error");
        if (!error.isMissingNode()) {
            throw new RuntimeException(error.path("message").asText("API error"));
        }
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        JsonNode usage = root.path("usage");
        int prompt = usage.path("prompt_tokens").asInt(0);
        int completion = usage.path("completion_tokens").asInt(0);
        return AiModelResponse.builder()
                .content(content)
                .promptTokens(prompt)
                .completionTokens(completion)
                .totalTokens(usage.path("total_tokens").asInt(prompt + completion))
                .build();
    }
}
