package com.app.civify.service.ai.provider;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.service.ai.AiProvider;
import com.app.civify.service.ai.client.ByokChatCompletionsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.providers.openrouter.enabled", havingValue = "true", matchIfMissing = true)
public class OpenRouterAiProvider implements AiProvider {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";

    private final ByokChatCompletionsClient chatClient;

    @Override
    public AiModelResponse callModel(String prompt, String model) {
        throw new UnsupportedOperationException("OpenRouter requires a BYOK API key");
    }

    @Override
    public AiModelResponse callModelWithApiKey(String prompt, String model, String apiKeyOverride) {
        return chatClient.complete(BASE_URL, apiKeyOverride, model, prompt);
    }

    @Override
    public String getProviderName() {
        return "openrouter";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
