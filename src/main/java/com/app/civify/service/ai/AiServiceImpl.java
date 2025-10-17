package com.app.civify.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    private final GeminiAiClient geminiAiClient;
    private final Map<String, AiProvider> providers = new ConcurrentHashMap<>();

    @Value("${ai.default.provider:gemini}")
    private String defaultProvider;

    @Value("${ai.default.model:gemini-pro}")
    private String defaultModel;

    // Constructor for Spring injection
    public AiServiceImpl(GeminiAiClient geminiAiClient,
            @Value("${ai.default.provider:gemini}") String defaultProvider,
            @Value("${ai.default.model:gemini-pro}") String defaultModel) {
        this.geminiAiClient = geminiAiClient;
        this.defaultProvider = defaultProvider;
        this.defaultModel = defaultModel;
        // Register providers
        providers.put("gemini", new GeminiProvider(geminiAiClient));
        // Add more providers as needed
    }

    @Override
    public String parseCv(MultipartFile file) {
        log.info("Parsing CV file: " + file.getOriginalFilename());
        try {
            // Implement CV parsing logic using AI
            String prompt = "Parse this CV and extract key information including skills, experience, education, etc.";
            return callModel(prompt, defaultProvider, defaultModel);
        } catch (Exception e) {
            log.info("Error parsing CV: " + e.getMessage());
            throw new RuntimeException("Failed to parse CV", e);
        }
    }

    @Override
    public String tailorCv(Long cvId, String jobDescription) {
        log.info("Tailoring CV " + cvId + " for job description");
        try {
            String prompt = String.format("Tailor this CV for the following job description: %s", jobDescription);
            return callModel(prompt, defaultProvider, defaultModel);
        } catch (Exception e) {
            log.info("Error tailoring CV: " + e.getMessage());
            throw new RuntimeException("Failed to tailor CV", e);
        }
    }

    @Override
    public String callModel(String prompt, String provider, String model) {
        log.info("Calling AI model - Provider: " + provider + ", Model: " + model);
        try {
            AiProvider aiProvider = providers.get(provider);
            if (aiProvider == null) {
                throw new IllegalArgumentException("Unsupported AI provider: " + provider);
            }
            return aiProvider.callModel(prompt, model);
        } catch (Exception e) {
            log.info("Error calling AI model: " + e.getMessage());
            throw new RuntimeException("Failed to call AI model", e);
        }
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
