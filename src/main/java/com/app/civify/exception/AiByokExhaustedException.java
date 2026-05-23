package com.app.civify.exception;

import com.app.civify.entity.enums.ByokMode;
import lombok.Getter;

import java.util.List;

@Getter
public class AiByokExhaustedException extends RuntimeException {

    private final ByokMode mode;
    private final List<ByokAttemptFailure> attempts;
    private final boolean platformFallbackAvailable;

    public AiByokExhaustedException(
            ByokMode mode,
            List<ByokAttemptFailure> attempts,
            boolean platformFallbackAvailable) {
        super("All configured BYOK providers failed for this call.");
        this.mode = mode;
        this.attempts = attempts;
        this.platformFallbackAvailable = platformFallbackAvailable;
    }

    public record ByokAttemptFailure(
            String credentialId,
            String provider,
            String model,
            String errorCode) {}
}
