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
     * First tries to parse usageMetadata from the response JSON.
     * Falls back to counting the actual text content tokens for generic provider support.
     * @param aiResponse Raw JSON response from AI provider
     * @return int array [inputTokens, outputTokens], uses approximation if metadata not available
     */
    public int[] extractTokenUsage(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return new int[] { 0, 0 };
        }
        
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            
            // Try Gemini-specific metadata first
            JsonNode usage = root.path("usageMetadata");
            if (!usage.isMissingNode() && usage.has("promptTokenCount")) {
                int inputTokens = usage.path("promptTokenCount").asInt(0);
                int outputTokens = usage.path("candidatesTokenCount").asInt(0);
                log.debug("Extracted token usage from Gemini metadata - input: {}, output: {}", inputTokens, outputTokens);
                return new int[] { inputTokens, outputTokens };
            }
            
            // Try OpenAI-style metadata
            JsonNode openAiUsage = root.path("usage");
            if (!openAiUsage.isMissingNode() && openAiUsage.has("prompt_tokens")) {
                int inputTokens = openAiUsage.path("prompt_tokens").asInt(0);
                int outputTokens = openAiUsage.path("completion_tokens").asInt(0);
                log.debug("Extracted token usage from OpenAI metadata - input: {}, output: {}", inputTokens, outputTokens);
                return new int[] { inputTokens, outputTokens };
            }
            
            // Fallback: Extract text content and count tokens using tokenizer
            String textContent = extractTextContent(root);
            if (textContent != null && !textContent.isEmpty()) {
                int outputTokens = countTokens(textContent);
                log.debug("Approximated output tokens from text content: {}", outputTokens);
                // Input tokens unknown in fallback, return 0 for input
                return new int[] { 0, outputTokens };
            }
            
            log.warn("Could not extract token usage - no metadata or text content found");
            return new int[] { 0, 0 };
            
        } catch (Exception e) {
            log.warn("Failed to extract token usage from response: {}", e.getMessage());
            return new int[] { 0, 0 };
        }
    }
    
    /**
     * Extract text content from AI response for fallback token counting.
     */
    private String extractTextContent(JsonNode root) {
        // Try Gemini format: candidates[0].content.parts[0].text
        JsonNode geminiText = root.at("/candidates/0/content/parts/0/text");
        if (!geminiText.isMissingNode()) {
            return geminiText.asText();
        }
        
        // Try OpenAI format: choices[0].message.content
        JsonNode openAiText = root.at("/choices/0/message/content");
        if (!openAiText.isMissingNode()) {
            return openAiText.asText();
        }
        
        return null;
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
