package com.app.civify.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhanced AI service with multi-model and multi-provider support Features: -
 * Multiple AI providers (Gemini, OpenAI, Vertex AI, etc.) - Automatic fallback to
 * secondary provider - Retry mechanism with exponential backoff -
 * Provider-specific error handling
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final Map<String, AiProvider> providers;

    public AiServiceImpl(List<AiProvider> aiProviders) {
        
        // Auto-discover and register providers
        this.providers = aiProviders.stream()
                .collect(Collectors.toMap(
                    p -> p.getProviderName().toLowerCase(),
                    Function.identity(),
                    (existing, replacement) -> existing, 
                    ConcurrentHashMap::new
                ));

        log.info("AI Service initialized with providers: {}", providers.keySet());
    }

    // parseCv and tailorCv methods removed (logic moved to respective services)

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500, multiplier = 2))
    @Cacheable(value = "ai_responses", key = "{#prompt, #provider, #model}") 
    public String callModel(String prompt, String provider, String model) {
        log.info("Calling AI model - Provider: {}, Model: {}", provider, model);

        AiProvider aiProvider = providers.get(provider.toLowerCase());
        if (aiProvider == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + provider
                    + ". Available providers: " + providers.keySet());
        }
        
        if (!aiProvider.isAvailable()) {
             throw new IllegalStateException("AI provider '" + provider + "' is not configured properly (missing API key/config).");
        }

        try {
            String response = aiProvider.callModel(prompt, model);
            log.debug("AI model call successful - Provider: {}, Response length: {}", provider, response.length());
            return response;
        } catch (Exception e) {
            log.error("Error calling AI model (Provider: {}, Model: {}): {}", provider, model, e.getMessage());
            throw new RuntimeException("Failed to call AI model: " + e.getMessage(), e);
        }

    }

    /**
     * Recovery method called after all retry attempts fail
     */
    @Recover
    public String recoverAiCall(Exception e, String prompt, String provider, String model) {
        log.error("All retry attempts exhausted for AI call. Provider: {}, Model: {}, Error: {}",
                provider, model, e.getMessage());
        throw new RuntimeException(
                "AI service is currently unavailable. All retry attempts failed. Please try again later.",
                e
        );
    }
}
