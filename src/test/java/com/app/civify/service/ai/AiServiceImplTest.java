//package com.app.civify.service.ai;
//
//import com.app.civify.dto.ai.AiModelResponse;
//import com.app.civify.service.ai.AiProvider;
//import com.app.civify.service.ai.AiServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AiServiceImplTest {
//
//    @Mock
//    private AiProvider geminiProvider;
//
//    @Mock
//    private AiProvider vertexProvider;
//
//    private AiServiceImpl aiService;
//
//    @BeforeEach
//    void setUp() {
//        // Setup providers
//        when(geminiProvider.getProviderName()).thenReturn("gemini");
//        when(vertexProvider.getProviderName()).thenReturn("vertex");
//
//        // Initialize service with mocked providers
//        aiService = new AiServiceImpl(List.of(geminiProvider, vertexProvider));
//    }
//
//    @Test
//    void callModel_Success_UseSpecificProvider() {
//        // Given
//        String prompt = "Test prompt";
//        String provider = "gemini";
//        String model = "gemini-pro";
//
//        AiModelResponse expectedResponse = AiModelResponse.builder()
//                .content("AI Response Content")
//                .totalTokens(150)
//                .build();
//
//        when(geminiProvider.isAvailable()).thenReturn(true);
//        when(geminiProvider.callModel(eq(prompt), eq(model)))
//                .thenReturn(expectedResponse);
//
//        // When
//        AiModelResponse result = aiService.callModel(prompt, provider, model);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("AI Response Content", result.getContent());
//        assertEquals(150, result.getTotalTokens());
//        verify(geminiProvider).callModel(eq(prompt), eq(model));
//        verify(vertexProvider, never()).callModel(anyString(), anyString());
//    }
//
//    @Test
//    void callModel_ThrowsException_WhenProviderNotConfigured() {
//        // Given
//        String prompt = "Test prompt";
//        String provider = "gemini";
//        String model = "gemini-pro";
//
//        when(geminiProvider.isAvailable()).thenReturn(false);
//
//        // When & Then
//        IllegalStateException exception = assertThrows(IllegalStateException.class,
//                () -> aiService.callModel(prompt, provider, model));
//        assertTrue(exception.getMessage().contains("not configured"));
//    }
//
//    @Test
//    void callModel_ThrowsException_WhenProviderNotFound() {
//        // Given
//        String prompt = "Test prompt";
//        String provider = "unknown-provider";
//        String model = "model";
//
//        // When & Then
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> aiService.callModel(prompt, provider, model));
//        assertTrue(exception.getMessage().contains("Unsupported AI provider"));
//    }
//}
