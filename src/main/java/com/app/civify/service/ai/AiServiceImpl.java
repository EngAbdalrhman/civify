package com.app.civify.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
//@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final GeminiAiClient geminiAiClient;
    private final Map<String, AiProvider> providers = new ConcurrentHashMap<>();

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    @Value("${ai.fallback.provider:}")
    private String fallbackProvider;

    // Constructor for Spring injection
    public AiServiceImpl(GeminiAiClient geminiAiClient,
            @Value("${ai.default.provider:gemini}") String defaultProvider,
            @Value("${ai.default.model:gemini-pro}") String defaultModel,
            @Value("${ai.fallback.provider:}") String fallbackProvider) {
        this.geminiAiClient = geminiAiClient;
        this.defaultProvider = defaultProvider;
        this.defaultModel = defaultModel;
        this.fallbackProvider = fallbackProvider;
        // Register providers
        providers.put("gemini", new GeminiProvider(geminiAiClient));
        // Add more providers as needed
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String parseCv(MultipartFile file) {
        log.info("Parsing CV file: {}", file.getOriginalFilename());
        try {
            // Implement CV parsing logic using AI
            String prompt = "Parse this CV and extract key information including skills, experience, education, etc.";
            return callModelWithFallback(prompt, defaultProvider, defaultModel);
        } catch (Exception e) {
            log.error("Error parsing CV: {}", e.getMessage());
            throw new RuntimeException("Failed to parse CV", e);
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String tailorCv(Long cvId, String jobDescription) {
        log.info("Tailoring CV {} for job description", cvId);
        try {
            String prompt = String.format("Tailor this CV for the following job description: %s", jobDescription);
            return callModelWithFallback(prompt, defaultProvider, defaultModel);
        } catch (Exception e) {
            log.error("Error tailoring CV: {}", e.getMessage());
            throw new RuntimeException("Failed to tailor CV", e);
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public String callModel(String prompt, String provider, String model) {
        log.info("Calling AI model - Provider: {}, Model: {}", provider, model);
        try {
            AiProvider aiProvider = providers.get(provider);
            if (aiProvider == null) {
                throw new IllegalArgumentException("Unsupported AI provider: " + provider);
            }
            return aiProvider.callModel(prompt, model);
        } catch (Exception e) {
            log.error("Error calling AI model: {}", e.getMessage());
            throw new RuntimeException("Failed to call AI model", e);
        }
    }

    /**
     * Call AI model with fallback provider support
     */
    private String callModelWithFallback(String prompt, String provider, String model) {
        try {
            return callModel(prompt, provider, model);
        } catch (Exception e) {
            if (fallbackProvider != null && !fallbackProvider.isEmpty() && !fallbackProvider.equals(provider)) {
                log.warn("Primary AI provider {} failed, trying fallback provider {}", provider, fallbackProvider);
                try {
                    return callModel(prompt, fallbackProvider, model);
                } catch (Exception fallbackException) {
                    log.error("Fallback AI provider {} also failed: {}", fallbackProvider, fallbackException.getMessage());
                    throw new RuntimeException("Both primary and fallback AI providers failed", fallbackException);
                }
            } else {
                throw e;
            }
        }
    }

    @Recover
    public String recoverAiCall(Exception e, String prompt, String provider, String model) {
        log.error("All retry attempts failed for AI call. Provider: {}, Model: {}, Error: {}", provider, model, e.getMessage());
        throw new RuntimeException("AI service is currently unavailable. Please try again later.", e);
    }

    // Provider interface
    public interface AiProvider {

        String callModel(String prompt, String model);
    }

    // Gemini provider implementation
    public static class GeminiProvider implements AiProvider {

        private final GeminiAiClient client;

        public GeminiProvider(GeminiAiClient client) {
            this.client = client;
        }

        @Override
        public String callModel(String prompt, String model) {
            // Implement Gemini-specific logic
            String requestBody = String.format("{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}", prompt);
            return client.callModel(model, requestBody);
        }
    }
}
