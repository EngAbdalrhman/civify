// package com.app.civify.service.auth;

// import com.app.civify.entity.User;
// import com.app.civify.exception.BadRequestException;
// import com.app.civify.repository.UserRepository;
// import com.app.civify.service.subscription.SubscriptionService;
// import com.app.civify.util.EmailService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.data.redis.core.ValueOperations;
// import org.springframework.test.util.ReflectionTestUtils;

// import java.time.Duration;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class EmailVerificationServiceTest {

//     @Mock
//     private StringRedisTemplate redisTemplate;

//     @Mock
//     private ValueOperations<String, String> valueOperations;

//     @Mock
//     private EmailService emailService;

//     @Mock
//     private UserRepository userRepository;

//     @Mock
//     private SubscriptionService subscriptionService;

//     @InjectMocks
//     private EmailVerificationService emailVerificationService;

//     private User testUser;

//     @BeforeEach
//     void setUp() {
//         // Set private fields using ReflectionTestUtils
//         ReflectionTestUtils.setField(emailVerificationService, "verificationLinkBase",
//                 "http://localhost:8080/api/v1/auth/verify-email");

//         testUser = new User();
//         testUser.setId(1L);
//         testUser.setEmail("test@example.com");
//         testUser.setUsername("testuser");
//         testUser.setIsEmailVerified(false);

//         when(redisTemplate.opsForValue()).thenReturn(valueOperations);
//     }

//     @Test
//     void sendVerificationEmail_Success() {
//         // When
//         emailVerificationService.sendVerificationEmail(testUser);

//         // Then
//         ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
//         ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
//         ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

//         verify(valueOperations).set(keyCaptor.capture(), emailCaptor.capture(), durationCaptor.capture());
//         verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());

//         assertTrue(keyCaptor.getValue().startsWith("email_verification:"));
//         assertEquals("test@example.com", emailCaptor.getValue());
//         assertEquals(Duration.ofHours(24), durationCaptor.getValue());
//     }

//     @Test
//     void verifyEmail_Success() {
//         // Given
//         String token = "test-token-123";
//         String email = "test@example.com";

//         when(valueOperations.get("email_verification:" + token)).thenReturn(email);
//         when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
//         when(userRepository.save(any(User.class))).thenReturn(testUser);

//         // When
//         boolean result = emailVerificationService.verifyEmail(token);

//         // Then
//         assertTrue(result);
//         assertTrue(testUser.getIsEmailVerified());
//         verify(userRepository).save(testUser);
//         verify(subscriptionService).createDefaultFreeSubscription(1L);
//         verify(redisTemplate).delete("email_verification:" + token);
//     }

//     @Test
//     void verifyEmail_InvalidToken() {
//         // Given
//         String token = "invalid-token";
//         when(valueOperations.get("email_verification:" + token)).thenReturn(null);

//         // When
//         boolean result = emailVerificationService.verifyEmail(token);

//         // Then
//         assertFalse(result);
//         verify(userRepository, never()).save(any());
//         verify(subscriptionService, never()).createDefaultFreeSubscription(anyLong());
//     }

//     @Test
//     void verifyEmail_UserNotFound() {
//         // Given
//         String token = "test-token-123";
//         String email = "nonexistent@example.com";

//         when(valueOperations.get("email_verification:" + token)).thenReturn(email);
//         when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

//         // When/Then
//         assertThrows(BadRequestException.class, () -> emailVerificationService.verifyEmail(token));
//         verify(subscriptionService, never()).createDefaultFreeSubscription(anyLong());
//     }

//     @Test
//     void resendVerificationEmail_Success() {
//         // Given
//         String email = "test@example.com";
//         when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

//         // When
//         emailVerificationService.resendVerificationEmail(email);

//         // Then
//         verify(valueOperations).set(anyString(), eq(email), eq(Duration.ofHours(24)));
//         verify(emailService).sendEmail(eq(email), anyString(), anyString());
//     }

//     @Test
//     void resendVerificationEmail_UserNotFound() {
//         // Given
//         String email = "nonexistent@example.com";
//         when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

//         // When/Then
//         assertThrows(BadRequestException.class,
//                 () -> emailVerificationService.resendVerificationEmail(email));
//         verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
//     }

//     @Test
//     void resendVerificationEmail_AlreadyVerified() {
//         // Given
//         testUser.setIsEmailVerified(true);
//         when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

//         // When/Then
//         assertThrows(BadRequestException.class,
//                 () -> emailVerificationService.resendVerificationEmail("test@example.com"));
//         verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
//     }

//     @Test
//     void verificationLink_ContainsToken() {
//         // When
//         emailVerificationService.sendVerificationEmail(testUser);

//         // Then
//         ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
//         verify(emailService).sendEmail(eq("test@example.com"), anyString(), bodyCaptor.capture());

//         String emailBody = bodyCaptor.getValue();
//         assertTrue(emailBody.contains("http://localhost:8080/api/v1/auth/verify-email?token="));
//     }
// }
