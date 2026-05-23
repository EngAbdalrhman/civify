package com.app.civify.service.ai.credential;

import com.app.civify.entity.enums.AiCredentialSource;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ResolvedAiCall {
    String providerName;
    String model;
    String apiKeyOverride;
    AiCredentialSource source;
    UUID credentialId;
    boolean platformPath;
}
