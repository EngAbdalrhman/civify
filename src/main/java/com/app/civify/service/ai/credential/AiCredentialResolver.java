package com.app.civify.service.ai.credential;

import com.app.civify.config.AiByokProperties;
import com.app.civify.dto.ai.credential.ByokFallbackEntry;
import com.app.civify.entity.ai.UserAiCredential;
import com.app.civify.entity.ai.UserAiSettings;
import com.app.civify.entity.enums.AiCredentialSource;
import com.app.civify.entity.enums.AiCredentialStatus;
import com.app.civify.entity.enums.ByokMode;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.repository.ai.UserAiCredentialRepository;
import com.app.civify.repository.ai.UserAiSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCredentialResolver {

    private final AiByokProperties byokProperties;
    private final AiCredentialService credentialService;
    private final UserAiCredentialRepository credentialRepository;
    private final UserAiSettingsRepository settingsRepository;

    /**
     * Ordered call chain for this request. Last entry may be platform path when mode is OPPORTUNISTIC.
     */
    public ResolvedCredentialChain resolve(
            Long userId,
            TokenUsageType usageType,
            String platformProvider,
            String platformModel) {

        ResolvedAiCall platform = ResolvedAiCall.builder()
                .providerName(platformProvider.toLowerCase())
                .model(platformModel)
                .apiKeyOverride(null)
                .source(AiCredentialSource.PLATFORM)
                .credentialId(null)
                .platformPath(true)
                .build();

        if (!byokProperties.isEnabled() || userId == null) {
            return new ResolvedCredentialChain(List.of(platform), ByokMode.DISABLED);
        }

        UserAiSettings settings = findSettings(userId);
        ByokMode mode = settings.getByokMode();

        if (mode == ByokMode.DISABLED) {
            return new ResolvedCredentialChain(List.of(platform), mode);
        }

        List<ResolvedAiCall> chain = new ArrayList<>();
        List<ByokFallbackEntry> entries = settings.getFallbackChain() != null
                ? settings.getFallbackChain()
                : List.of();

        for (ByokFallbackEntry entry : entries) {
            buildByokCall(userId, entry).ifPresent(chain::add);
        }

        if (mode == ByokMode.OPPORTUNISTIC) {
            if (isPlatformProviderAllowed(platformProvider)) {
                chain.add(platform);
            } else {
                log.warn("Platform provider '{}' not in allowlist; skipping platform fallback", platformProvider);
            }
        }

        if (chain.isEmpty()) {
            if (mode == ByokMode.STRICT) {
                return new ResolvedCredentialChain(List.of(), mode);
            }
            return new ResolvedCredentialChain(List.of(platform), mode);
        }

        return new ResolvedCredentialChain(chain, mode);
    }

    private UserAiSettings findSettings(Long userId) {
        if (userId == null) {
            return defaultSettings(null);
        }
        return settingsRepository.findById(userId).orElseGet(() -> defaultSettings(userId));
    }

    private static UserAiSettings defaultSettings(Long userId) {
        UserAiSettings settings = new UserAiSettings();
        settings.setUserId(userId);
        settings.setByokMode(ByokMode.DISABLED);
        settings.setFallbackChain(new ArrayList<>());
        return settings;
    }

    private Optional<ResolvedAiCall> buildByokCall(Long userId, ByokFallbackEntry entry) {
        if (entry.getCredentialId() == null) {
            return Optional.empty();
        }
        Optional<UserAiCredential> opt = credentialRepository.findByIdAndUserId(entry.getCredentialId(), userId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        UserAiCredential credential = opt.get();
        if (credential.getStatus() != AiCredentialStatus.ACTIVE) {
            return Optional.empty();
        }
        String model = entry.getModelOverride() != null && !entry.getModelOverride().isBlank()
                ? entry.getModelOverride()
                : credential.getDefaultModel();
        if (model == null || model.isBlank()) {
            log.warn("Skipping BYOK credential {} — no model configured", credential.getId());
            return Optional.empty();
        }
        try {
            String apiKey = credentialService.decryptApiKey(credential);
            return Optional.of(ResolvedAiCall.builder()
                    .providerName(credential.getProvider().toProviderName())
                    .model(model)
                    .apiKeyOverride(apiKey)
                    .source(AiCredentialSource.USER_BYOK)
                    .credentialId(credential.getId())
                    .platformPath(false)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to decrypt BYOK credential {}: {}", credential.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isPlatformProviderAllowed(String provider) {
        return byokProperties.getPlatformProvidersAllowed().stream()
                .anyMatch(p -> p.equalsIgnoreCase(provider));
    }
}
