package com.app.civify.service.ai;

import com.app.civify.dto.ai.AiModelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhanced AI service with multi-provider support and automatic failover.
 * 
 * On primary provider failure (429, 503, or any error), automatically
 * falls back to the configured fallback provider. This is the industry
 * best practice — retrying a rate-limited provider is futile since
 * the rate limit window (typically 60s) won't have passed.
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final Map<String, AiProvider> providers;

    @Value("${ai.fallback.provider:gemini}")
    private String fallbackProviderName;

    @Value("${ai.fallback.model:gemini-2.5-flash}")
    private String fallbackModel;

    public AiServiceImpl(List<AiProvider> aiProviders) {
        this.providers = aiProviders.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        ConcurrentHashMap::new));

        log.info("AI Service initialized with providers: {}", providers.keySet());
    }

    @Override
    public AiModelResponse callModel(String prompt, String provider, String model) {
        log.info("Calling AI model - Provider: {}, Model: {}", provider, model);

        // 1. Try primary provider
        try {
            return callProvider(provider, model, prompt);
        } catch (Exception primaryEx) {
            log.warn("Primary provider '{}' failed: {}. Attempting fallback to '{}'...",
                    provider, primaryEx.getMessage(), fallbackProviderName);

            // 2. If primary fails, try fallback provider (skip if same as primary)
            if (fallbackProviderName.equalsIgnoreCase(provider)) {
                log.error("Fallback provider is the same as primary ('{}'), no alternative available.", provider);
                throw new RuntimeException(
                        "AI service unavailable. Provider '" + provider + "' failed: " + primaryEx.getMessage(),
                        primaryEx);
            }

            try {
                AiModelResponse fallbackResponse = callProvider(fallbackProviderName, fallbackModel, prompt);
                log.info("Fallback provider '{}' succeeded after primary '{}' failed.",
                        fallbackProviderName, provider);
                return fallbackResponse;
            } catch (Exception fallbackEx) {
                log.error("Both AI providers failed. Primary '{}': {}. Fallback '{}': {}.",
                        provider, primaryEx.getMessage(),
                        fallbackProviderName, fallbackEx.getMessage());
                throw new RuntimeException(
                        "AI service unavailable. Primary ('" + provider + "'): " + primaryEx.getMessage()
                                + ". Fallback ('" + fallbackProviderName + "'): " + fallbackEx.getMessage(),
                        fallbackEx);
            }
        }
    }

    /**
     * Call a specific provider with validation.
     */
    private AiModelResponse callProvider(String providerName, String model, String prompt) {
        AiProvider aiProvider = providers.get(providerName.toLowerCase());
        if (aiProvider == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + providerName
                    + ". Available providers: " + providers.keySet());
        }

        if (!aiProvider.isAvailable()) {
            throw new IllegalStateException(
                    "AI provider '" + providerName + "' is not configured properly (missing API key/config).");
        }

        try {
            AiModelResponse response = aiProvider.callModel(prompt, model);
            log.debug("AI model call successful - Provider: {}, Tokens: {}", providerName, response.getTotalTokens());
            return response;
        } catch (Exception e) {
            log.error("Error calling AI model (Provider: {}, Model: {}): {}", providerName, model, e.getMessage());
            throw new RuntimeException("Failed to call AI model: " + e.getMessage(), e);
        }
    }
}
