package com.app.civify.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private GeminiAiClient geminiAiClient;

    @Mock
    private MultipartFile mockFile;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(geminiAiClient, "gemini", "gemini-pro", "openai");
    }

    @Test
    void testParseCv_Success() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(geminiAiClient.callModel(anyString(), anyString())).thenReturn("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Parsed CV content\"}]}}]}");

        // When
        String result = aiService.parseCv(mockFile);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Parsed CV content"));
        verify(geminiAiClient).callModel(anyString(), anyString());
    }

    @Test
    void testParseCv_FailureWithRetry() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(geminiAiClient.callModel(anyString(), anyString()))
                .thenThrow(new RuntimeException("API Error"))
                .thenReturn("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Parsed CV content\"}]}}]}");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> aiService.parseCv(mockFile));
        assertEquals("Failed to parse CV", exception.getMessage());
        verify(geminiAiClient, times(3)).callModel(anyString(), anyString());
    }

    @Test
    void testParseCv_AllRetriesFail() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(geminiAiClient.callModel(anyString(), anyString()))
                .thenThrow(new RuntimeException("API Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> aiService.parseCv(mockFile));
        assertEquals("Failed to parse CV", exception.getMessage());
        verify(geminiAiClient, times(3)).callModel(anyString(), anyString()); // 3 retries
    }

    @Test
    void testTailorCv_Success() {
        // Given
        Long cvId = 1L;
        String jobDescription = "Software Engineer position";
        when(geminiAiClient.callModel(anyString(), anyString())).thenReturn("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Tailored CV content\"}]}}]}");

        // When
        String result = aiService.tailorCv(cvId, jobDescription);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Tailored CV content"));
        verify(geminiAiClient).callModel(anyString(), anyString());
    }

    @Test
    void testCallModel_Success() {
        // Given
        String prompt = "Test prompt";
        String provider = "gemini";
        String model = "gemini-pro";
        when(geminiAiClient.callModel(anyString(), anyString())).thenReturn("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Response\"}]}}]}");

        // When
        String result = aiService.callModel(prompt, provider, model);

        // Then
        assertNotNull(result);
        verify(geminiAiClient).callModel(anyString(), anyString());
    }

    @Test
    void testCallModel_UnsupportedProvider() {
        // Given
        String prompt = "Test prompt";
        String provider = "unsupported";
        String model = "model";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> aiService.callModel(prompt, provider, model));
        assertEquals("Unsupported AI provider: unsupported", exception.getMessage());
    }

    @Test
    void testProviderSwitching_FallbackSuccess() {
        // Given - Primary provider fails, fallback succeeds
        String prompt = "Test prompt";

        // Mock primary provider failure and fallback success
        when(geminiAiClient.callModel(anyString(), anyString()))
                .thenThrow(new RuntimeException("Primary provider failed"))
                .thenReturn("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Fallback response\"}]}}]}");

        // When
        String result = aiService.callModel(prompt, "gemini", "gemini-pro");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Fallback response"));
        verify(geminiAiClient, times(2)).callModel(anyString(), anyString());
    }

    @Test
    void testProviderSwitching_BothFail() {
        // Given - Both primary and fallback fail
        String prompt = "Test prompt";
        when(geminiAiClient.callModel(anyString(), anyString()))
                .thenThrow(new RuntimeException("Both providers failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> aiService.callModel(prompt, "gemini", "gemini-pro"));
        assertEquals("Failed to call AI model", exception.getMessage());
    }

    @Test
    void testRecoverMethod() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(geminiAiClient.callModel(anyString(), anyString()))
                .thenThrow(new RuntimeException("Persistent API Error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> aiService.parseCv(mockFile));
        assertEquals("Failed to parse CV", exception.getMessage());
    }
}
