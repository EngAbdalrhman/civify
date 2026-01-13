package com.app.civify.service.ai;

/**
 * Interface that all AI providers must implement
 */
public interface AiProvider {

    /**
     * Call the AI model with given prompt
     *
     * @param prompt The prompt to send
     * @param model The specific model to use
     * @return AI response
     */
    String callModel(String prompt, String model);

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
