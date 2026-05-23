package com.app.civify.service.ai;

import com.app.civify.config.AiByokProperties;
import com.app.civify.dto.ai.AiModelResponse;
import com.app.civify.entity.enums.AiCredentialSource;
import com.app.civify.entity.enums.ByokMode;
import com.app.civify.service.ai.credential.AiCredentialResolver;
import com.app.civify.service.ai.credential.ResolvedAiCall;
import com.app.civify.service.ai.credential.ResolvedCredentialChain;
import com.app.civify.service.ai.usage.AiCredentialUsageRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private AiProvider geminiProvider;

    @Mock
    private AiProvider vertexProvider;

    @Mock
    private AiCredentialResolver credentialResolver;

    @Mock
    private AiCredentialUsageRecorder usageRecorder;

    private AiByokProperties byokProperties;
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        when(geminiProvider.getProviderName()).thenReturn("gemini");
        when(vertexProvider.getProviderName()).thenReturn("vertex");

        byokProperties = new AiByokProperties();
        byokProperties.setEnabled(false);
        byokProperties.setPlatformProvidersAllowed(Set.of("gemini", "vertex"));

        aiService = new AiServiceImpl(
                List.of(geminiProvider, vertexProvider),
                byokProperties,
                credentialResolver,
                usageRecorder);
        ReflectionTestUtils.setField(aiService, "fallbackProviderName", "gemini");
        ReflectionTestUtils.setField(aiService, "fallbackModel", "gemini-2.5-flash");
    }

    @Test
    void callModel_whenByokDisabled_usesPlatformPathOnly() {
        String prompt = "Test prompt";
        String provider = "gemini";
        String model = "gemini-pro";

        AiModelResponse expected = AiModelResponse.builder()
                .content("AI Response Content")
                .totalTokens(150)
                .build();

        when(geminiProvider.isAvailable()).thenReturn(true);
        when(geminiProvider.callModel(eq(prompt), eq(model))).thenReturn(expected);

        AiModelResponse result = aiService.callModel(prompt, provider, model);

        assertNotNull(result);
        assertEquals("AI Response Content", result.getContent());
        assertEquals(AiCredentialSource.PLATFORM, result.getCredentialSource());
        verify(geminiProvider).callModel(eq(prompt), eq(model));
        verify(credentialResolver, never()).resolve(any(), any(), any(), any());
    }

    @Test
    void callModel_whenByokEnabledButNoAuth_usesPlatformPath() {
        byokProperties.setEnabled(true);

        AiModelResponse expected = AiModelResponse.builder().content("ok").totalTokens(10).build();
        when(vertexProvider.isAvailable()).thenReturn(true);
        when(vertexProvider.callModel(anyString(), anyString())).thenReturn(expected);

        AiModelResponse result = aiService.callModel("p", "vertex", "gemini-3-flash-preview");
        assertEquals("ok", result.getContent());
        assertEquals(AiCredentialSource.PLATFORM, result.getCredentialSource());
        verify(credentialResolver, never()).resolve(any(), any(), any(), any());
    }

    @Test
    void callModel_platformPath_fallsBackOnPrimaryFailure() {
        String prompt = "Test";
        when(vertexProvider.isAvailable()).thenReturn(true);
        when(vertexProvider.callModel(anyString(), anyString()))
                .thenThrow(new RuntimeException("rate limited"));
        when(geminiProvider.isAvailable()).thenReturn(true);
        when(geminiProvider.callModel(anyString(), eq("gemini-2.5-flash")))
                .thenReturn(AiModelResponse.builder().content("fallback").build());

        AiModelResponse result = aiService.callModel(prompt, "vertex", "gemini-3-flash-preview");
        assertEquals("fallback", result.getContent());
        assertEquals(AiCredentialSource.PLATFORM, result.getCredentialSource());
    }
}
