package com.app.civify.service.ai;

import com.app.civify.dto.ai.AiModelResponse;

/**
 * Interface that all AI providers must implement
 */
public interface AiProvider {



    /**
     * Call the AI model with given prompt
     *
     * @param prompt The prompt to send
     * @param model The specific model to use
     * @return AI response with content and token metadata
     */
    AiModelResponse callModel(String prompt, String model);

    /**
     * Call using a user-supplied API key (BYOK). Default impl delegates to platform call.
     */
    default AiModelResponse callModelWithApiKey(String prompt, String model, String apiKeyOverride) {
        if (apiKeyOverride != null && !apiKeyOverride.isBlank()) {
            throw new UnsupportedOperationException(
                    "BYOK not supported for provider: " + getProviderName());
        }
        return callModel(prompt, model);
    }

    /**
     * Get provider name
     */
    default String getProviderName() {
        return this.getClass().getSimpleName().replace("AiProvider", "").replace("Provider", "").toLowerCase();
    }

    /**
     * Check if provider is available/configured
     */
    default boolean isAvailable() {
        return true;
    }
}
