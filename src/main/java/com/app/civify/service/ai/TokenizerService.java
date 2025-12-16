package com.app.civify.service.ai;

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
}
