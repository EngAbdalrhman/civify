// package com.app.civify.service.ai;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.web.multipart.MultipartFile;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class AiServiceImplTest {

//     @Mock
//     private GeminiAiClient geminiAiClient;

//     @Mock
//     private MultipartFile mockFile;

//     private AiServiceImpl aiService;

//     @BeforeEach
//     void setUp() {
//         // Initialize with gemini as primary, no fallback
//         aiService = new AiServiceImpl(geminiAiClient, "gemini", "gemini-pro", "", "");
//     }

//     @Test
//     void parseCv_Success() {
//         // Given
//         when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
//         when(geminiAiClient.callModel(anyString(), anyString()))
//                 .thenReturn("Parsed CV content in JSON format");

//         // When
//         String result = aiService.parseCv(mockFile);

//         // Then
//         assertNotNull(result);
//         assertEquals("Parsed CV content in JSON format", result);
//         verify(geminiAiClient).callModel(eq("gemini-pro"), anyString());
//     }

//     @Test
//     void parseCv_ThrowsException() {
//         // Given
//         when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
//         when(geminiAiClient.callModel(anyString(), anyString()))
//                 .thenThrow(new RuntimeException("API Error"));

//         // When & Then
//         RuntimeException exception = assertThrows(RuntimeException.class,
//                 () -> aiService.parseCv(mockFile));
//         assertTrue(exception.getMessage().contains("Failed to parse CV"));
//     }

//     @Test
//     void tailorCv_Success() {
//         // Given
//         Long cvId = 1L;
//         String jobDescription = "Software Engineer with Java and Spring Boot experience";
//         when(geminiAiClient.callModel(anyString(), anyString()))
//                 .thenReturn("Tailored CV content optimized for the job");

//         // When
//         String result = aiService.tailorCv(cvId, jobDescription);

//         // Then
//         assertNotNull(result);
//         assertEquals("Tailored CV content optimized for the job", result);
//         verify(geminiAiClient).callModel(eq("gemini-pro"), contains(jobDescription));
//     }

//     @Test
//     void tailorCv_ThrowsException() {
//         // Given
//         Long cvId = 1L;
//         String jobDescription = "Test job";
//         when(geminiAiClient.callModel(anyString(), anyString()))
//                 .thenThrow(new RuntimeException("API Error"));

//         // When & Then
//         RuntimeException exception = assertThrows(RuntimeException.class,
//                 () -> aiService.tailorCv(cvId, jobDescription));
//         assertTrue(exception.getMessage().contains("Failed to tailor CV"));
//     }

//     @Test
//     void callModel_Success() {
//         // Given
//         String prompt = "Test prompt";
//         String provider = "gemini";
//         String model = "gemini-pro";
//         when(geminiAiClient.callModel(eq(model), anyString()))
//                 .thenReturn("AI response");

//         // When
//         String result = aiService.callModel(prompt, provider, model);

//         // Then
//         assertNotNull(result);
//         assertEquals("AI response", result);
//         verify(geminiAiClient).callModel(eq(model), anyString());
//     }

//     @Test
//     void callModel_UnsupportedProvider() {
//         // Given
//         String prompt = "Test prompt";
//         String provider = "unsupported";
//         String model = "model";

//         // When & Then
//         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                 () -> aiService.callModel(prompt, provider, model));
//         assertTrue(exception.getMessage().contains("Unsupported AI provider"));
//         assertTrue(exception.getMessage().contains("unsupported"));
//     }

//     @Test
//     void callModel_WithFallback_PrimaryFails_FallbackSucceeds() {
//         // Given - Initialize with fallback provider
//         AiServiceImpl serviceWithFallback = new AiServiceImpl(
//                 geminiAiClient, "gemini", "gemini-pro", "gemini", "gemini-1.5-pro"
//         );

//         when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
//         when(geminiAiClient.callModel(eq("gemini-pro"), anyString()))
//                 .thenThrow(new RuntimeException("Primary model failed"));
//         when(geminiAiClient.callModel(eq("gemini-1.5-pro"), anyString()))
//                 .thenReturn("Fallback response");

//         // When
//         String result = serviceWithFallback.parseCv(mockFile);

//         // Then
//         assertNotNull(result);
//         assertEquals("Fallback response", result);
//         verify(geminiAiClient).callModel(eq("gemini-pro"), anyString());
//         verify(geminiAiClient).callModel(eq("gemini-1.5-pro"), anyString());
//     }

//     @Test
//     void callModel_WithFallback_BothFail() {
//         // Given - Initialize with fallback provider
//         AiServiceImpl serviceWithFallback = new AiServiceImpl(
//                 geminiAiClient, "gemini", "gemini-pro", "gemini", "gemini-1.5-pro"
//         );

//         when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
//         when(geminiAiClient.callModel(anyString(), anyString()))
//                 .thenThrow(new RuntimeException("API Error"));

//         // When & Then
//         RuntimeException exception = assertThrows(RuntimeException.class,
//                 () -> serviceWithFallback.parseCv(mockFile));
//         assertTrue(exception.getMessage().contains("Both primary"));
//         assertTrue(exception.getMessage().contains("fallback"));
//     }

//     @Test
//     void registerProvider_Success() {
//         // Given
//         AiServiceImpl.AiProvider mockProvider = mock(AiServiceImpl.AiProvider.class);
//         when(mockProvider.getProviderName()).thenReturn("TestProvider");

//         // When
//         aiService.registerProvider("test", mockProvider);

//         // Then - Should not throw exception
//         // Provider is registered successfully
//     }

//     @Test
//     void geminiProvider_CallModel_Success() {
//         // Given
//         AiServiceImpl.GeminiProvider provider = new AiServiceImpl.GeminiProvider(geminiAiClient);
//         String prompt = "Test prompt";
//         String model = "gemini-pro";
//         when(geminiAiClient.callModel(eq(model), anyString()))
//                 .thenReturn("Response from Gemini");

//         // When
//         String result = provider.callModel(prompt, model);

//         // Then
//         assertNotNull(result);
//         assertEquals("Response from Gemini", result);
//         verify(geminiAiClient).callModel(eq(model), anyString());
//     }

//     @Test
//     void geminiProvider_GetProviderName() {
//         // Given
//         AiServiceImpl.GeminiProvider provider = new AiServiceImpl.GeminiProvider(geminiAiClient);

//         // When
//         String name = provider.getProviderName();

//         // Then
//         assertEquals("Gemini", name);
//     }

//     @Test
//     void geminiProvider_CallModel_HandlesSpecialCharacters() {
//         // Given
//         AiServiceImpl.GeminiProvider provider = new AiServiceImpl.GeminiProvider(geminiAiClient);
//         String promptWithQuotes = "Test \"quoted\" text\nWith newline";
//         String model = "gemini-pro";
//         when(geminiAiClient.callModel(eq(model), anyString()))
//                 .thenReturn("Response");

//         // When
//         String result = provider.callModel(promptWithQuotes, model);

//         // Then
//         assertNotNull(result);
//         verify(geminiAiClient).callModel(eq(model), contains("\\\"quoted\\\""));
//         verify(geminiAiClient).callModel(eq(model), contains("\\n"));
//     }

//     @Test
//     void geminiProvider_CallModel_ThrowsException() {
//         // Given
//         AiServiceImpl.GeminiProvider provider = new AiServiceImpl.GeminiProvider(geminiAiClient);
//         when(geminiAiClient.callModel(anyString(), anyString()))
//                 .thenThrow(new RuntimeException("Connection error"));

//         // When & Then
//         RuntimeException exception = assertThrows(RuntimeException.class,
//                 () -> provider.callModel("prompt", "model"));
//         assertTrue(exception.getMessage().contains("Gemini API call failed"));
//     }
// }
