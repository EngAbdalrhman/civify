package com.app.civify.service.ai.credential;

import com.app.civify.config.AiByokProperties;
import com.app.civify.dto.ai.credential.ByokFallbackEntry;
import com.app.civify.entity.ai.UserAiCredential;
import com.app.civify.entity.ai.UserAiSettings;
import com.app.civify.entity.enums.AiCredentialSource;
import com.app.civify.entity.enums.AiCredentialStatus;
import com.app.civify.entity.enums.AiProviderType;
import com.app.civify.entity.enums.ByokMode;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.repository.ai.UserAiCredentialRepository;
import com.app.civify.repository.ai.UserAiSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiCredentialResolverTest {

    @Mock
    private AiCredentialService credentialService;

    @Mock
    private UserAiCredentialRepository credentialRepository;

    @Mock
    private UserAiSettingsRepository settingsRepository;

    private AiByokProperties byokProperties;
    private AiCredentialResolver resolver;

    @BeforeEach
    void setUp() {
        byokProperties = new AiByokProperties();
        byokProperties.setEnabled(true);
        byokProperties.setPlatformProvidersAllowed(Set.of("gemini", "vertex"));
        resolver = new AiCredentialResolver(
                byokProperties, credentialService, credentialRepository, settingsRepository);
    }

    @Test
    void resolve_whenByokDisabled_returnsPlatformOnly() {
        byokProperties.setEnabled(false);
        ResolvedCredentialChain chain = resolver.resolve(1L, TokenUsageType.OTHER, "gemini", "gemini-pro");
        assertEquals(1, chain.getCalls().size());
        assertTrue(chain.getCalls().get(0).isPlatformPath());
        verify(settingsRepository, never()).findById(any());
    }

    @Test
    void resolve_whenModeDisabled_returnsPlatformOnly() {
        UserAiSettings settings = settings(1L, ByokMode.DISABLED, List.of());
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        ResolvedCredentialChain chain = resolver.resolve(1L, TokenUsageType.OTHER, "gemini", "gemini-pro");
        assertEquals(1, chain.getCalls().size());
        assertTrue(chain.getCalls().get(0).isPlatformPath());
    }

    @Test
    void resolve_opportunistic_withByokKey_appendsPlatformFallback() {
        UUID credId = UUID.randomUUID();
        UserAiSettings settings = settings(1L, ByokMode.OPPORTUNISTIC, List.of(
                ByokFallbackEntry.builder().credentialId(credId).build()));
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        UserAiCredential credential = activeCredential(credId, 1L);
        when(credentialRepository.findByIdAndUserId(credId, 1L)).thenReturn(Optional.of(credential));
        when(credentialService.decryptApiKey(credential)).thenReturn("sk-test");

        ResolvedCredentialChain chain = resolver.resolve(1L, TokenUsageType.CV_TAILOR, "gemini", "gemini-pro");

        assertEquals(2, chain.getCalls().size());
        assertEquals(AiCredentialSource.USER_BYOK, chain.getCalls().get(0).getSource());
        assertTrue(chain.getCalls().get(1).isPlatformPath());
    }

    @Test
    void resolve_strict_emptyChain_returnsEmpty() {
        UserAiSettings settings = settings(1L, ByokMode.STRICT, List.of());
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        ResolvedCredentialChain chain = resolver.resolve(1L, TokenUsageType.OTHER, "gemini", "gemini-pro");
        assertTrue(chain.getCalls().isEmpty());
        assertTrue(chain.isStrict());
    }

    @Test
    void resolve_strict_withValidKey_noPlatformFallback() {
        UUID credId = UUID.randomUUID();
        UserAiSettings settings = settings(1L, ByokMode.STRICT, List.of(
                ByokFallbackEntry.builder().credentialId(credId).build()));
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        UserAiCredential credential = activeCredential(credId, 1L);
        when(credentialRepository.findByIdAndUserId(credId, 1L)).thenReturn(Optional.of(credential));
        when(credentialService.decryptApiKey(credential)).thenReturn("sk-test");

        ResolvedCredentialChain chain = resolver.resolve(1L, TokenUsageType.OTHER, "gemini", "gemini-pro");

        assertEquals(1, chain.getCalls().size());
        assertFalse(chain.getCalls().get(0).isPlatformPath());
    }

    @Test
    void resolve_opportunistic_noSettingsRow_usesDefaultsWithoutInsert() {
        when(settingsRepository.findById(1L)).thenReturn(Optional.empty());

        ResolvedCredentialChain chain = resolver.resolve(1L, TokenUsageType.OTHER, "gemini", "gemini-pro");

        assertEquals(1, chain.getCalls().size());
        assertTrue(chain.getCalls().get(0).isPlatformPath());
        verify(settingsRepository, never()).save(any());
    }

    private static UserAiSettings settings(Long userId, ByokMode mode, List<ByokFallbackEntry> chain) {
        UserAiSettings settings = new UserAiSettings();
        settings.setUserId(userId);
        settings.setByokMode(mode);
        settings.setFallbackChain(chain);
        return settings;
    }

    private static UserAiCredential activeCredential(UUID id, Long userId) {
        UserAiCredential credential = new UserAiCredential();
        credential.setId(id);
        credential.setUserId(userId);
        credential.setProvider(AiProviderType.OPENAI);
        credential.setLabel("primary");
        credential.setDefaultModel("gpt-4o-mini");
        credential.setStatus(AiCredentialStatus.ACTIVE);
        return credential;
    }
}
