package com.app.civify.dto.ai.credential;

import com.app.civify.entity.enums.AiCredentialStatus;
import com.app.civify.entity.enums.AiProviderType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class UserAiCredentialResponse {
    UUID id;
    AiProviderType provider;
    String label;
    String keyFingerprint;
    String defaultModel;
    AiCredentialStatus status;
    LocalDateTime lastValidatedAt;
    String lastError;
}
