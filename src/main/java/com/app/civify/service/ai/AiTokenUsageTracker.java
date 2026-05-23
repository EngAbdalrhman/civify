package com.app.civify.service.ai;

import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.entity.enums.AiCredentialSource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks total and BYOK token usage across parallel AI calls (e.g. CV tailor).
 */
public class AiTokenUsageTracker {

    private final AtomicInteger totalTokens = new AtomicInteger(0);
    private final AtomicInteger byokTokens = new AtomicInteger(0);

    public void add(AiModelResponse response) {
        if (response == null) {
            return;
        }
        int tokens = response.getTotalTokens();
        if (tokens <= 0) {
            return;
        }
        totalTokens.addAndGet(tokens);
        if (response.getCredentialSource() == AiCredentialSource.USER_BYOK) {
            byokTokens.addAndGet(tokens);
        }
    }

    public int getTotalTokens() {
        return totalTokens.get();
    }

    public int getByokTokens() {
        return byokTokens.get();
    }
}
