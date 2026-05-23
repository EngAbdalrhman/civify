package com.app.civify.service.ai.usage;

import com.app.civify.entity.ai.AiCredentialUsageLog;
import com.app.civify.entity.enums.AiCredentialSource;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.repository.ai.AiCredentialUsageLogRepository;
import com.app.civify.util.SensitiveMessageScrubber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCredentialUsageRecorder {

    private final AiCredentialUsageLogRepository repository;

    @Async
    public void record(
            Long userId,
            Long organizationId,
            UUID credentialId,
            AiCredentialSource source,
            String provider,
            String model,
            TokenUsageType usageType,
            int promptTokens,
            int completionTokens,
            int latencyMs,
            boolean success,
            String errorCode,
            String errorMessage) {
        try {
            AiCredentialUsageLog entry = AiCredentialUsageLog.builder()
                    .userId(userId)
                    .organizationId(organizationId)
                    .credentialId(credentialId)
                    .source(source)
                    .provider(provider)
                    .model(model)
                    .tokenUsageType(usageType != null ? usageType : TokenUsageType.OTHER)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .latencyMs(latencyMs)
                    .success(success)
                    .errorCode(errorCode)
                    .errorMessage(truncate(SensitiveMessageScrubber.scrub(errorMessage), 500))
                    .build();
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to record AI credential usage: {}", e.getMessage());
        }
    }

    private static String truncate(String msg, int max) {
        if (msg == null) {
            return null;
        }
        return msg.length() <= max ? msg : msg.substring(0, max);
    }
}
