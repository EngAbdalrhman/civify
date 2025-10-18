//package com.app.civify.service.subscription;
//
//import com.app.civify.entity.User;
//import com.app.civify.entity.UserSubscription;
//import com.app.civify.repository.UserSubscriptionRepository;
//import com.app.civify.util.EmailService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
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
//    private SubscriptionLifecycleService lifecycleService;
//
//    @BeforeEach
//    void setUp() {
//        lifecycleService = new SubscriptionLifecycleService(
//                subscriptionRepository, subscriptionService, emailService);
//    }
//
//    @Test
//    void testProcessExpiredSubscriptions() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime pastDate = now.minusDays(1);
//
//        UserSubscription expiredSubscription = createMockSubscription();
//        lenient().when(expiredSubscription.getStatus()).thenReturn(UserSubscription.SubscriptionStatus.ACTIVE);
//        lenient().when(expiredSubscription.getEndDate()).thenReturn(pastDate);
//
//        lenient().when(subscriptionRepository.findByStatusAndEndDateBefore(
//                UserSubscription.SubscriptionStatus.ACTIVE, any(LocalDateTime.class)))
//                .thenReturn(List.of(expiredSubscription));
//
//        // When
//        lifecycleService.processExpiredSubscriptions();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBefore(
//                eq(UserSubscription.SubscriptionStatus.ACTIVE), any(LocalDateTime.class));
//        verify(expiredSubscription).setStatus(UserSubscription.SubscriptionStatus.EXPIRED);
//        verify(subscriptionRepository).save(expiredSubscription);
//        verify(subscriptionService).downgradeUserToFreePlan(any(Long.class));
//    }
//
//    @Test
//    void testProcessOverduePayments() {
//        // Given
//        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
//
//        UserSubscription overdueSubscription = createMockSubscription();
//        lenient().when(overdueSubscription.getStatus()).thenReturn(UserSubscription.SubscriptionStatus.PENDING);
//        lenient().when(overdueSubscription.getStartDate()).thenReturn(oneWeekAgo.minusDays(1));
//
//        lenient().when(subscriptionRepository.findByStatusAndEndDateBefore(
//                UserSubscription.SubscriptionStatus.PENDING, any(LocalDateTime.class)))
//                .thenReturn(List.of(overdueSubscription));
//
//        // When
//        lifecycleService.processOverduePayments();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBefore(
//                eq(UserSubscription.SubscriptionStatus.PENDING), any(LocalDateTime.class));
//        verify(overdueSubscription).setStatus(UserSubscription.SubscriptionStatus.CANCELLED);
//        verify(subscriptionRepository).save(overdueSubscription);
//        verify(subscriptionService).downgradeUserToFreePlan(any(Long.class));
//    }
//
//    @Test
//    void testProcessPaymentReminders() {
//        // Given
//        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
//
//        UserSubscription pendingSubscription = createMockSubscription();
//        lenient().when(pendingSubscription.getStatus()).thenReturn(UserSubscription.SubscriptionStatus.PENDING);
//        lenient().when(pendingSubscription.getStartDate()).thenReturn(threeDaysAgo.minusDays(1));
//
//        lenient().when(subscriptionRepository.findByStatusAndStartDateBefore(
//                UserSubscription.SubscriptionStatus.PENDING, any(LocalDateTime.class)))
//                .thenReturn(List.of(pendingSubscription));
//
//        // When
//        lifecycleService.processPaymentReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndStartDateBefore(
//                eq(UserSubscription.SubscriptionStatus.PENDING), any(LocalDateTime.class));
//        verify(emailService).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void testSendRenewalReminders() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime threeDaysFromNow = now.plusDays(3);
//
//        UserSubscription expiringSubscription = createMockSubscription();
//        lenient().when(expiringSubscription.getStatus()).thenReturn(UserSubscription.SubscriptionStatus.ACTIVE);
//        lenient().when(expiringSubscription.getEndDate()).thenReturn(now.plusDays(2)); // Expires in 2 days
//
//        lenient().when(subscriptionRepository.findByStatusAndEndDateBetween(
//                UserSubscription.SubscriptionStatus.ACTIVE, any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(expiringSubscription));
//
//        // When
//        lifecycleService.sendRenewalReminders();
//
//        // Then
//        verify(subscriptionRepository).findByStatusAndEndDateBetween(
//                eq(UserSubscription.SubscriptionStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class));
//        verify(emailService).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void testSendRenewalReminder_EmailFailure() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime threeDaysFromNow = now.plusDays(3);
//
//        UserSubscription expiringSubscription = createMockSubscription();
//        lenient().when(expiringSubscription.getStatus()).thenReturn(UserSubscription.SubscriptionStatus.ACTIVE);
//        lenient().when(expiringSubscription.getEndDate()).thenReturn(now.plusDays(2));
//
//        lenient().when(subscriptionRepository.findByStatusAndEndDateBetween(
//                UserSubscription.SubscriptionStatus.ACTIVE, any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(List.of(expiringSubscription));
//
//        doThrow(new RuntimeException("Email service error"))
//                .when(emailService).sendEmail(anyString(), anyString(), anyString());
//
//        // When - Should not throw exception
//        lifecycleService.sendRenewalReminders();
//
//        // Then - Email service was called but exception was handled
//        verify(emailService).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    @Test
//    void testSendPaymentReminder_EmailFailure() {
//        // Given
//        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
//
//        UserSubscription pendingSubscription = createMockSubscription();
//        lenient().when(pendingSubscription.getStatus()).thenReturn(UserSubscription.SubscriptionStatus.PENDING);
//        lenient().when(pendingSubscription.getStartDate()).thenReturn(threeDaysAgo.minusDays(1));
//
//        lenient().when(subscriptionRepository.findByStatusAndStartDateBefore(
//                UserSubscription.SubscriptionStatus.PENDING, any(LocalDateTime.class)))
//                .thenReturn(List.of(pendingSubscription));
//
//        doThrow(new RuntimeException("Email service error"))
//                .when(emailService).sendEmail(anyString(), anyString(), anyString());
//
//        // When - Should not throw exception
//        lifecycleService.processPaymentReminders();
//
//        // Then - Email service was called but exception was handled
//        verify(emailService).sendEmail(anyString(), anyString(), anyString());
//    }
//
//    private UserSubscription createMockSubscription() {
//        UserSubscription subscription = mock(UserSubscription.class);
//        User user = mock(User.class);
//
//        when(subscription.getUser()).thenReturn(user);
//        when(user.getId()).thenReturn(1L);
//        when(user.getUsername()).thenReturn("testuser");
//        when(user.getEmail()).thenReturn("test@example.com");
//
//        return subscription;
//    }
//}
