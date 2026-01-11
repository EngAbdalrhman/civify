package com.app.civify.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for accurate LLM token counting using jtokkit.
 * Uses cl100k_base encoding (GPT-4, GPT-3.5-turbo compatible).
 */
@Service
@Slf4j
public class TokenizerService {

    private Encoding encoding;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Default multiplier for output tokens (output costs ~4x more than input).
     */
    public static final int OUTPUT_TOKEN_MULTIPLIER = 4;

    @PostConstruct
    public void init() {
        try {
            EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
            this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
            log.info("✅ Tokenizer initialized with cl100k_base encoding");
        } catch (Exception e) {
            log.error("❌ Failed to initialize tokenizer, falling back to heuristic", e);
            this.encoding = null;
        }
    }

    /**
     * Count tokens in text using accurate tokenizer.
     * Falls back to heuristic (chars/4) if tokenizer fails.
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        if (encoding != null) {
            try {
                return encoding.countTokens(text);
            } catch (Exception e) {
                log.warn("Token counting failed, using heuristic: {}", e.getMessage());
            }
        }

        // Fallback: chars/4 heuristic
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    /**
     * Estimate cost for a given number of tokens.
     * Can be customized based on AI provider pricing.
     */
    public double estimateCost(int tokens, double costPerMillionTokens) {
        return (tokens / 1_000_000.0) * costPerMillionTokens;
    }

    /**
     * Extract token usage from Gemini API response.
     * Parses the usageMetadata field from the response JSON.
     * @param geminiResponse Raw JSON response from Gemini API
     * @return int array [inputTokens, outputTokens], returns [0, 0] if parsing fails
     */
    public int[] extractTokenUsage(String geminiResponse) {
        if (geminiResponse == null || geminiResponse.isEmpty()) {
            return new int[] { 0, 0 };
        }
        
        try {
            JsonNode root = objectMapper.readTree(geminiResponse);
            JsonNode usage = root.path("usageMetadata");
            
            int inputTokens = usage.path("promptTokenCount").asInt(0);
            int outputTokens = usage.path("candidatesTokenCount").asInt(0);
            
            log.debug("Extracted token usage - input: {}, output: {}", inputTokens, outputTokens);
            return new int[] { inputTokens, outputTokens };
        } catch (Exception e) {
            log.warn("Failed to extract token usage from response: {}", e.getMessage());
            return new int[] { 0, 0 };
        }
    }

    /**
     * Calculate billable tokens with output multiplier.
     * Output tokens cost approximately 4x more than input tokens.
     * @param inputTokens Number of input/prompt tokens
     * @param outputTokens Number of output/completion tokens
     * @return Total billable tokens (input + output * multiplier)
     */
    public int calculateBillableTokens(int inputTokens, int outputTokens) {
        return inputTokens + (outputTokens * OUTPUT_TOKEN_MULTIPLIER);
    }

    /**
     * Calculate billable tokens with custom multiplier.
     */
    public int calculateBillableTokens(int inputTokens, int outputTokens, int outputMultiplier) {
        return inputTokens + (outputTokens * outputMultiplier);
    }
}
