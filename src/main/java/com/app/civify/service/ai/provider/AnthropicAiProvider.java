package com.app.civify.service.ai.provider;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.service.ai.AiProvider;
import com.app.civify.service.ai.client.ByokAnthropicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.providers.anthropic.enabled", havingValue = "true", matchIfMissing = true)
public class AnthropicAiProvider implements AiProvider {

    private final ByokAnthropicClient anthropicClient;

    @Override
    public AiModelResponse callModel(String prompt, String model) {
        throw new UnsupportedOperationException("Anthropic BYOK requires a user API key");
    }

    @Override
    public AiModelResponse callModelWithApiKey(String prompt, String model, String apiKeyOverride) {
        return anthropicClient.complete(apiKeyOverride, model, prompt);
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
