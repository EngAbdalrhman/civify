//package com.app.civify.service.subscription;
//
//import com.app.civify.entity.SubscriptionPlan;
//import com.app.civify.entity.User;
//import com.app.civify.entity.UserSubscription;
//import com.app.civify.entity.enums.SubscriptionStatus;
//import com.app.civify.repository.UserSubscriptionRepository;
//import com.app.civify.util.EmailService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SubscriptionLifecycleServiceTest {
//
//    @Mock
//    private UserSubscriptionRepository subscriptionRepository;
//
//    @Mock
//    private SubscriptionService subscriptionService;
//
//    @Mock
//    private EmailService emailService;
//
//    @InjectMocks
//    private SubscriptionLifecycleService lifecycleService;
//
//    private User testUser;
//    private SubscriptionPlan testPlan;
//    private UserSubscription testSubscription;
//
//    @BeforeEach
//    void setUp() {
//        testUser = new User();
//        testUser.setId(1L);
//        testUser.setUsername("testuser");
//        testUser.setEmail("test@example.com");
//
//        testPlan = new SubscriptionPlan();
//        testPlan.setId(1L);
//        testPlan.setName("Premium");
//
//        testSubscription = new UserSubscription();
//        testSubscription.setId(1L);
//        testSubscription.setUser(testUser);
//        testSubscription.setPlan(testPlan);
//        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
//    }
//
//    @Test
//    void processExpiredSubscriptions_Success() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        testSubscription.setEndDate(now.minusDays(1));
//
//        when(subscriptionRepository.findByStatusAndEndDateBefore(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//
//        // When
//        lifecycleService.processExpiredSubscriptions();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBefore(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class));
//        verify(subscriptionRepository).save(testSubscription);
//        verify(subscriptionService).reactivateFreePlan(1L);
//        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
//    }
//
//    @Test
//    void processExpiredSubscriptions_NoExpiredSubscriptions() {
//        // Given
//        when(subscriptionRepository.findByStatusAndEndDateBefore(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
//                .thenReturn(List.of());
//
//        // When
//        lifecycleService.processExpiredSubscriptions();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBefore(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class));
//        verify(subscriptionRepository, never()).save(any());
//        verify(subscriptionService, never()).reactivateFreePlan(anyLong());
//    }
//
//    @Test
//    void processExpiredSubscriptions_HandlesErrors() {
//        // Given
//        testSubscription.setEndDate(LocalDateTime.now().minusDays(1));
//
//        when(subscriptionRepository.findByStatusAndEndDateBefore(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//        doThrow(new RuntimeException("Email service error"))
//                .when(emailService).sendEmail(anyString(), anyString(), anyString());
//
//        // When - Should not throw exception, error should be logged
//        lifecycleService.processExpiredSubscriptions();
//
//        // Then
//        verify(subscriptionRepository).save(testSubscription);
//        verify(subscriptionService).reactivateFreePlan(1L);
//    }
//
//    @Test
//    void processOverduePayments_Success() {
//        // Given
//        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
//        testSubscription.setStatus(SubscriptionStatus.PENDING);
//        testSubscription.setStartDate(oneWeekAgo.minusDays(1));
//
//        when(subscriptionRepository.findByStatusAndStartDateBefore(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//
//        // When
//        lifecycleService.processOverduePayments();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndStartDateBefore(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class));
//        verify(subscriptionRepository).save(testSubscription);
//        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
//    }
//
//    @Test
//    void processOverduePayments_NoOverduePayments() {
//        // Given
//        when(subscriptionRepository.findByStatusAndStartDateBefore(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class)))
//                .thenReturn(List.of());
//
//        // When
//        lifecycleService.processOverduePayments();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndStartDateBefore(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class));
//        verify(subscriptionRepository, never()).save(any());
//        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void processPaymentReminders_Success() {
//        // Given
//        LocalDateTime fourDaysAgo = LocalDateTime.now().minusDays(4);
//        testSubscription.setStatus(SubscriptionStatus.PENDING);
//        testSubscription.setStartDate(fourDaysAgo);
//
//        when(subscriptionRepository.findByStatusAndStartDateBetween(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//
//        // When
//        lifecycleService.processPaymentReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndStartDateBetween(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class));
//        verify(emailService).sendEmail(eq("test@example.com"), anyString(), contains("Payment Reminder"));
//    }
//
//    @Test
//    void processPaymentReminders_NoRemindersNeeded() {
//        // Given
//        when(subscriptionRepository.findByStatusAndStartDateBetween(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of());
//
//        // When
//        lifecycleService.processPaymentReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndStartDateBetween(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class));
//        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void processPaymentReminders_HandlesEmailFailure() {
//        // Given
//        LocalDateTime fourDaysAgo = LocalDateTime.now().minusDays(4);
//        testSubscription.setStatus(SubscriptionStatus.PENDING);
//        testSubscription.setStartDate(fourDaysAgo);
//
//        when(subscriptionRepository.findByStatusAndStartDateBetween(
//                eq(SubscriptionStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//        doThrow(new RuntimeException("Email service error"))
//                .when(emailService).sendEmail(anyString(), anyString(), anyString());
//
//        // When - Should not throw exception
//        lifecycleService.processPaymentReminders();
//
//        // Then
//        verify(emailService).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void sendRenewalReminders_Success() {
//        // Given
//        LocalDateTime twoDaysFromNow = LocalDateTime.now().plusDays(2);
//        testSubscription.setEndDate(twoDaysFromNow);
//        testPlan.setName("Premium"); // Non-free plan
//
//        when(subscriptionRepository.findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//
//        // When
//        lifecycleService.sendRenewalReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class));
//        verify(emailService).sendEmail(eq("test@example.com"), anyString(), contains("Renewal Reminder"));
//    }
//
//    @Test
//    void sendRenewalReminders_SkipsFreePlan() {
//        // Given
//        LocalDateTime twoDaysFromNow = LocalDateTime.now().plusDays(2);
//        testSubscription.setEndDate(twoDaysFromNow);
//        testPlan.setName("Free"); // Free plan should be skipped
//
//        when(subscriptionRepository.findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//
//        // When
//        lifecycleService.sendRenewalReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class));
//        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void sendRenewalReminders_NoRemindersNeeded() {
//        // Given
//        when(subscriptionRepository.findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of());
//
//        // When
//        lifecycleService.sendRenewalReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class));
//        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void sendRenewalReminders_HandlesEmailFailure() {
//        // Given
//        LocalDateTime twoDaysFromNow = LocalDateTime.now().plusDays(2);
//        testSubscription.setEndDate(twoDaysFromNow);
//        testPlan.setName("Premium");
//
//        when(subscriptionRepository.findByStatusAndEndDateBetween(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(testSubscription));
//        doThrow(new RuntimeException("Email service error"))
//                .when(emailService).sendEmail(anyString(), anyString(), anyString());
//
//        // When - Should not throw exception
//        lifecycleService.sendRenewalReminders();
//
//        // Then
//        verify(emailService).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void processExpiredSubscriptions_MultipleSubscriptions() {
//        // Given
//        UserSubscription subscription1 = new UserSubscription();
//        subscription1.setId(1L);
//        subscription1.setUser(testUser);
//        subscription1.setPlan(testPlan);
//        subscription1.setStatus(SubscriptionStatus.ACTIVE);
//        subscription1.setEndDate(LocalDateTime.now().minusDays(1));
//
//        UserSubscription subscription2 = new UserSubscription();
//        subscription2.setId(2L);
//        subscription2.setUser(testUser);
//        subscription2.setPlan(testPlan);
//        subscription2.setStatus(SubscriptionStatus.ACTIVE);
//        subscription2.setEndDate(LocalDateTime.now().minusDays(2));
//
//        when(subscriptionRepository.findByStatusAndEndDateBefore(
//                eq(SubscriptionStatus.ACTIVE), any(LocalDateTime.class)))
//                .thenReturn(List.of(subscription1, subscription2));
//
//        // When
//        lifecycleService.processExpiredSubscriptions();
//
//        // Then
//        verify(subscriptionRepository, times(2)).save(any(UserSubscription.class));
//        verify(subscriptionService, times(2)).reactivateFreePlan(1L);
//        verify(emailService, times(2)).sendEmail(eq("test@example.com"), anyString(), anyString());
//    }
//}
