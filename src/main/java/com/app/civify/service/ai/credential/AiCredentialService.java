package com.app.civify.service.ai.credential;

import com.app.civify.config.AiByokProperties;
import com.app.civify.dto.ai.credential.*;
import com.app.civify.entity.ai.UserAiCredential;
import com.app.civify.entity.ai.UserAiSettings;
import com.app.civify.entity.enums.AiCredentialStatus;
import com.app.civify.entity.enums.AiProviderType;
import com.app.civify.entity.enums.ByokMode;
import com.app.civify.exception.BadRequestException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.ai.UserAiCredentialRepository;
import com.app.civify.repository.ai.UserAiSettingsRepository;
import com.app.civify.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCredentialService {

    private static final String PERM_BYOK_STRICT = "AI.BYOK_STRICT_MODE";

    private final UserAiCredentialRepository credentialRepository;
    private final UserAiSettingsRepository settingsRepository;
    private final AesGcmCipherService cipherService;
    private final ByokProbeService probeService;
    private final AiByokProperties byokProperties;
    private final UserService userService;

    @Transactional(readOnly = true)
    public UserAiSettingsResponse getSettings(Long userId) {
        UserAiSettings settings = getOrCreateSettings(userId);
        return toSettingsResponse(settings, canUseStrictMode());
    }

    @Transactional
    public UserAiSettingsResponse updateSettings(Long userId, UpdateUserAiSettingsRequest request) {
        UserAiSettings settings = getOrCreateSettings(userId);
        ByokMode mode = request.getByokMode() != null ? request.getByokMode() : settings.getByokMode();
        if (mode == ByokMode.STRICT && !canUseStrictMode()) {
            throw new BadRequestException("Strict BYOK mode requires AI.BYOK_STRICT_MODE permission");
        }
        validateFallbackChain(userId, request.getFallbackChain());
        settings.setByokMode(mode);
        settings.setFallbackChain(request.getFallbackChain() != null
                ? new ArrayList<>(request.getFallbackChain())
                : new ArrayList<>());
        settingsRepository.save(settings);
        return toSettingsResponse(settings, canUseStrictMode());
    }

    @Transactional(readOnly = true)
    public List<UserAiCredentialResponse> listCredentials(Long userId) {
        return credentialRepository.findByUserId(userId).stream()
                .map(this::toCredentialResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserAiCredentialResponse createCredential(Long userId, CreateUserAiCredentialRequest request) {
        requireEncryptionKey();
        probeService.probe(request.getProvider(), request.getApiKey(),
                request.getDefaultModel());

        AesGcmCipherService.EncryptedPayload encrypted =
                cipherService.encrypt(request.getApiKey(), byokProperties.getEncryptionKey());

        UserAiCredential credential = new UserAiCredential();
        credential.setUserId(userId);
        credential.setProvider(request.getProvider());
        credential.setLabel(request.getLabel().trim());
        credential.setCiphertext(encrypted.ciphertext());
        credential.setIv(encrypted.iv());
        credential.setKeyFingerprint(AesGcmCipherService.fingerprintApiKey(request.getApiKey()));
        credential.setDefaultModel(request.getDefaultModel());
        credential.setStatus(AiCredentialStatus.ACTIVE);
        credential.setLastValidatedAt(LocalDateTime.now());
        credential.setLastError(null);

        return toCredentialResponse(credentialRepository.save(credential));
    }

    @Transactional
    public UserAiCredentialResponse validateCredential(Long userId, UUID credentialId) {
        UserAiCredential credential = findOwned(userId, credentialId);
        String apiKey = decrypt(credential);
        try {
            probeService.probe(credential.getProvider(), apiKey, credential.getDefaultModel());
            credential.setStatus(AiCredentialStatus.ACTIVE);
            credential.setLastValidatedAt(LocalDateTime.now());
            credential.setLastError(null);
        } catch (Exception e) {
            credential.setStatus(AiCredentialStatus.INVALID);
            credential.setLastError(truncate(e.getMessage(), 255));
            credentialRepository.save(credential);
            throw new BadRequestException("API key validation failed: " + e.getMessage());
        }
        return toCredentialResponse(credentialRepository.save(credential));
    }

    @Transactional
    public UserAiCredentialResponse updateCredential(Long userId, UUID credentialId, UpdateUserAiCredentialRequest request) {
        UserAiCredential credential = findOwned(userId, credentialId);
        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            credential.setLabel(request.getLabel().trim());
        }
        if (request.getDefaultModel() != null) {
            credential.setDefaultModel(request.getDefaultModel().isBlank() ? null : request.getDefaultModel());
        }
        return toCredentialResponse(credentialRepository.save(credential));
    }

    @Transactional
    public void deleteCredential(Long userId, UUID credentialId) {
        UserAiCredential credential = findOwned(userId, credentialId);
        credential.setStatus(AiCredentialStatus.REVOKED);
        credentialRepository.save(credential);
        removeFromFallbackChain(userId, credentialId);
    }

    public String decryptApiKey(UserAiCredential credential) {
        requireEncryptionKey();
        return decrypt(credential);
    }

    public UserAiSettings getOrCreateSettings(Long userId) {
        return settingsRepository.findById(userId).orElseGet(() -> {
            UserAiSettings s = new UserAiSettings();
            s.setUserId(userId);
            s.setByokMode(ByokMode.DISABLED);
            s.setFallbackChain(new ArrayList<>());
            return settingsRepository.save(s);
        });
    }

    private void validateFallbackChain(Long userId, List<ByokFallbackEntry> chain) {
        if (chain == null) {
            return;
        }
        for (ByokFallbackEntry entry : chain) {
            if (entry.getCredentialId() == null) {
                throw new BadRequestException("Each fallback entry must include credentialId");
            }
            findOwned(userId, entry.getCredentialId());
        }
    }

    private void removeFromFallbackChain(Long userId, UUID credentialId) {
        UserAiSettings settings = settingsRepository.findById(userId).orElse(null);
        if (settings == null || settings.getFallbackChain() == null) {
            return;
        }
        List<ByokFallbackEntry> filtered = settings.getFallbackChain().stream()
                .filter(e -> !credentialId.equals(e.getCredentialId()))
                .collect(Collectors.toCollection(ArrayList::new));
        settings.setFallbackChain(filtered);
        settingsRepository.save(settings);
    }

    private UserAiCredential findOwned(Long userId, UUID id) {
        return credentialRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("AI credential not found"));
    }

    private String decrypt(UserAiCredential credential) {
        return cipherService.decrypt(
                credential.getCiphertext(),
                credential.getIv(),
                byokProperties.getEncryptionKey());
    }

    private void requireEncryptionKey() {
        if (byokProperties.getEncryptionKey() == null || byokProperties.getEncryptionKey().length() < 16) {
            throw new IllegalStateException("BYOK encryption key is not configured (civify.byok.encryption-key)");
        }
    }

    private boolean canUseStrictMode() {
        return userService.getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PERM_BYOK_STRICT::equals);
    }

    private UserAiSettingsResponse toSettingsResponse(UserAiSettings settings, boolean canUseStrict) {
        return UserAiSettingsResponse.builder()
                .byokMode(settings.getByokMode())
                .fallbackChain(settings.getFallbackChain() != null
                        ? settings.getFallbackChain() : List.of())
                .canUseStrictMode(canUseStrict)
                .build();
    }

    private UserAiCredentialResponse toCredentialResponse(UserAiCredential c) {
        return UserAiCredentialResponse.builder()
                .id(c.getId())
                .provider(c.getProvider())
                .label(c.getLabel())
                .keyFingerprint(c.getKeyFingerprint())
                .defaultModel(c.getDefaultModel())
                .status(c.getStatus())
                .lastValidatedAt(c.getLastValidatedAt())
                .lastError(c.getLastError())
                .build();
    }

    private static String truncate(String msg, int max) {
        if (msg == null) {
            return null;
        }
        return msg.length() <= max ? msg : msg.substring(0, max);
    }
}
