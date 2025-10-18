package com.app.civify.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced AI service with multi-model and multi-provider support
 * Features:
 * - Multiple AI providers (Gemini, OpenAI, etc.)
 * - Automatic fallback to secondary provider
 * - Retry mechanism with exponential backoff
 * - Provider-specific error handling
 */
@Service
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

    @Value("${ai.fallback.model:gemini-pro}")
    private String fallbackModel;

    // Constructor for Spring injection
    public AiServiceImpl(GeminiAiClient geminiAiClient,
            @Value("${ai.default.provider:gemini}") String defaultProvider,
            @Value("${ai.default.model:gemini-pro}") String defaultModel,
            @Value("${ai.fallback.provider:}") String fallbackProvider,
            @Value("${ai.fallback.model:gemini-pro}") String fallbackModel) {
        this.geminiAiClient = geminiAiClient;
        this.defaultProvider = defaultProvider;
        this.defaultModel = defaultModel;
        this.fallbackProvider = fallbackProvider;
        this.fallbackModel = fallbackModel;

        // Register providers
        registerProvider("gemini", new GeminiProvider(geminiAiClient));
        // Add more providers as they become available
        // registerProvider("openai", new OpenAiProvider(openAiClient));

        log.info("AI Service initialized with provider: {}, model: {}", defaultProvider, defaultModel);
        if (fallbackProvider != null && !fallbackProvider.isEmpty()) {
            log.info("Fallback provider configured: {}, model: {}", fallbackProvider, fallbackModel);
        }
    }

    /**
     * Register an AI provider
     */
    public void registerProvider(String name, AiProvider provider) {
        providers.put(name, provider);
        log.info("Registered AI provider: {}", name);
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String parseCv(MultipartFile file) {
        log.info("Parsing CV file: {}", file.getOriginalFilename());
        try {
            // Build comprehensive prompt for CV parsing
            String prompt = buildCvParsingPrompt();
            return callModelWithFallback(prompt, defaultProvider, defaultModel);
        } catch (Exception e) {
            log.error("Error parsing CV: {}", e.getMessage());
            throw new RuntimeException("Failed to parse CV", e);
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String tailorCv(Long cvId, String jobDescription) {
        log.info("Tailoring CV {} for job description", cvId);
        try {
            String prompt = buildCvTailoringPrompt(jobDescription);
            return callModelWithFallback(prompt, defaultProvider, defaultModel);
        } catch (Exception e) {
            log.error("Error tailoring CV: {}", e.getMessage());
            throw new RuntimeException("Failed to tailor CV", e);
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500, multiplier = 2))
    public String callModel(String prompt, String provider, String model) {
        log.info("Calling AI model - Provider: {}, Model: {}", provider, model);

        AiProvider aiProvider = providers.get(provider.toLowerCase());
        if (aiProvider == null) {
            throw new IllegalArgumentException("Unsupported AI provider: " + provider +
                    ". Available providers: " + providers.keySet());
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
     * Call AI model with automatic fallback to secondary provider
     */
    private String callModelWithFallback(String prompt, String provider, String model) {
        try {
            // Try primary provider
            return callModel(prompt, provider, model);
        } catch (Exception primaryException) {
            // Check if fallback is configured and different from primary
            if (fallbackProvider != null && !fallbackProvider.isEmpty()
                    && !fallbackProvider.equalsIgnoreCase(provider)) {

                log.warn("Primary AI provider '{}' failed with error: {}. Attempting fallback provider '{}'",
                        provider, primaryException.getMessage(), fallbackProvider);

                try {
                    // Try fallback provider with fallback model
                    String response = callModel(prompt, fallbackProvider, fallbackModel);
                    log.info("Fallback AI provider '{}' succeeded", fallbackProvider);
                    return response;

                } catch (Exception fallbackException) {
                    log.error("Fallback AI provider '{}' also failed: {}",
                            fallbackProvider, fallbackException.getMessage());

                    // Throw combined exception with both errors
                    throw new RuntimeException(
                            String.format("Both primary (%s) and fallback (%s) AI providers failed. " +
                                    "Primary error: %s, Fallback error: %s",
                                    provider, fallbackProvider,
                                    primaryException.getMessage(),
                                    fallbackException.getMessage()),
                            primaryException
                    );
                }
            } else {
                // No fallback configured or same as primary
                throw primaryException;
            }
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

    /**
     * Build comprehensive CV parsing prompt
     */
    private String buildCvParsingPrompt() {
        return """
                Parse the following CV/Resume and extract structured information in JSON format.
                Extract:
                - Personal Information (name, email, phone, location)
                - Professional Summary
                - Work Experience (company, role, duration, responsibilities, achievements)
                - Education (institution, degree, field, graduation year)
                - Skills (technical skills, soft skills)
                - Certifications
                - Languages
                - Projects (if any)

                Return the data in a well-structured JSON format.
                """;
    }

    /**
     * Build CV tailoring prompt
     */
    private String buildCvTailoringPrompt(String jobDescription) {
        return String.format("""
                You are an expert CV/Resume writer. Tailor the following CV to match this job description:

                Job Description:
                %s

                Tasks:
                1. Highlight relevant skills and experience that match the job requirements
                2. Reword bullet points to align with job description keywords
                3. Emphasize achievements relevant to the target role
                4. Maintain honesty - don't add false information
                5. Keep the same overall structure and format
                6. Optimize for ATS (Applicant Tracking Systems)

                Return the tailored CV maintaining professional formatting.
                """, jobDescription);
    }

    // ==================== Provider Interface and Implementations ====================

    /**
     * Interface that all AI providers must implement
     */
    public interface AiProvider {
        /**
         * Call the AI model with given prompt
         * @param prompt The prompt to send
         * @param model The specific model to use
         * @return AI response
         */
        String callModel(String prompt, String model);

        /**
         * Get provider name
         */
        default String getProviderName() {
            return this.getClass().getSimpleName().replace("Provider", "");
        }

        /**
         * Check if provider is available/configured
         */
        default boolean isAvailable() {
            return true;
        }
    }

    /**
     * Gemini AI Provider implementation
     */
    public static class GeminiProvider implements AiProvider {

        private final GeminiAiClient client;

        public GeminiProvider(GeminiAiClient client) {
            this.client = client;
        }

        @Override
        public String callModel(String prompt, String model) {
            try {
                // Escape quotes in prompt for JSON
                String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

                // Build Gemini API request
                String requestBody = String.format(
                        "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}",
                        escapedPrompt
                );

                return client.callModel(model, requestBody);

            } catch (Exception e) {
                throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
            }
        }

        @Override
        public String getProviderName() {
            return "Gemini";
        }
    }

    // Template for adding more providers:
    /*
    public static class OpenAiProvider implements AiProvider {

        private final OpenAiClient client;

        public OpenAiProvider(OpenAiClient client) {
            this.client = client;
        }

        @Override
        public String callModel(String prompt, String model) {
            // Implement OpenAI-specific logic
            String requestBody = String.format(
                    "{\"model\": \"%s\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                    model, prompt.replace("\"", "\\\"")
            );
            return client.chat(requestBody);
        }

        @Override
        public String getProviderName() {
            return "OpenAI";
        }
    }
    */
}
