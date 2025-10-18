package com.app.civify.service.subscription;

import com.app.civify.entity.User;
import com.app.civify.entity.UserSubscription;
import com.app.civify.repository.UserSubscriptionRepository;
import com.app.civify.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleServiceTest {

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EmailService emailService;

    private SubscriptionLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new SubscriptionLifecycleService(
                subscriptionRepository, subscriptionService, emailService);
    }

    @Test
    void testProcessExpiredSubscriptions() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastDate = now.minusDays(1);

        UserSubscription expiredSubscription = createMockSubscription();
        expiredSubscription.setStatus(UserSubscription.SubscriptionStatus.ACTIVE);
        expiredSubscription.setEndDate(pastDate);

        when(subscriptionRepository.findByStatusAndEndDateBefore(
                UserSubscription.SubscriptionStatus.ACTIVE, now))
                .thenReturn(List.of(expiredSubscription));

        // When
        lifecycleService.processExpiredSubscriptions();

        // Then
        verify(subscriptionRepository).findByStatusAndEndDateBefore(
                UserSubscription.SubscriptionStatus.ACTIVE, now);
        verify(expiredSubscription).setStatus(UserSubscription.SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).save(expiredSubscription);
        verify(subscriptionService).downgradeUserToFreePlan(any(Long.class));
    }

    @Test
    void testProcessOverduePayments() {
        // Given
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        UserSubscription overdueSubscription = createMockSubscription();
        overdueSubscription.setStatus(UserSubscription.SubscriptionStatus.PENDING);
        overdueSubscription.setStartDate(oneWeekAgo.minusDays(1));

        when(subscriptionRepository.findByStatusAndEndDateBefore(
                UserSubscription.SubscriptionStatus.PENDING, oneWeekAgo))
                .thenReturn(List.of(overdueSubscription));

        // When
        lifecycleService.processOverduePayments();

        // Then
        verify(subscriptionRepository).findByStatusAndEndDateBefore(
                UserSubscription.SubscriptionStatus.PENDING, oneWeekAgo);
        verify(overdueSubscription).setStatus(UserSubscription.SubscriptionStatus.CANCELLED);
        verify(subscriptionRepository).save(overdueSubscription);
        verify(subscriptionService).downgradeUserToFreePlan(any(Long.class));
    }

    @Test
    void testProcessPaymentReminders() {
        // Given
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        UserSubscription pendingSubscription = createMockSubscription();
        pendingSubscription.setStatus(UserSubscription.SubscriptionStatus.PENDING);
        pendingSubscription.setStartDate(threeDaysAgo.minusDays(1));

        when(subscriptionRepository.findByStatusAndStartDateBefore(
                UserSubscription.SubscriptionStatus.PENDING, threeDaysAgo))
                .thenReturn(List.of(pendingSubscription));

        // When
        lifecycleService.processPaymentReminders();

        // Then
        verify(subscriptionRepository).findByStatusAndStartDateBefore(
                UserSubscription.SubscriptionStatus.PENDING, threeDaysAgo);
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendRenewalReminders() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        UserSubscription expiringSubscription = createMockSubscription();
        expiringSubscription.setStatus(UserSubscription.SubscriptionStatus.ACTIVE);
        expiringSubscription.setEndDate(now.plusDays(2)); // Expires in 2 days

        when(subscriptionRepository.findByStatusAndEndDateBetween(
                UserSubscription.SubscriptionStatus.ACTIVE, now, threeDaysFromNow))
                .thenReturn(List.of(expiringSubscription));

        // When
        lifecycleService.sendRenewalReminders();

        // Then
        verify(subscriptionRepository).findByStatusAndEndDateBetween(
                UserSubscription.SubscriptionStatus.ACTIVE, now, threeDaysFromNow);
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendRenewalReminder_EmailFailure() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        UserSubscription expiringSubscription = createMockSubscription();
        expiringSubscription.setStatus(UserSubscription.SubscriptionStatus.ACTIVE);
        expiringSubscription.setEndDate(now.plusDays(2));

        when(subscriptionRepository.findByStatusAndEndDateBetween(
                UserSubscription.SubscriptionStatus.ACTIVE, now, threeDaysFromNow))
                .thenReturn(List.of(expiringSubscription));

        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When - Should not throw exception
        lifecycleService.sendRenewalReminders();

        // Then - Email service was called but exception was handled
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendPaymentReminder_EmailFailure() {
        // Given
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        UserSubscription pendingSubscription = createMockSubscription();
        pendingSubscription.setStatus(UserSubscription.SubscriptionStatus.PENDING);
        pendingSubscription.setStartDate(threeDaysAgo.minusDays(1));

        when(subscriptionRepository.findByStatusAndStartDateBefore(
                UserSubscription.SubscriptionStatus.PENDING, threeDaysAgo))
                .thenReturn(List.of(pendingSubscription));

        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When - Should not throw exception
        lifecycleService.processPaymentReminders();

        // Then - Email service was called but exception was handled
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    private UserSubscription createMockSubscription() {
        UserSubscription subscription = mock(UserSubscription.class);
        User user = mock(User.class);

        when(subscription.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("testuser");
        when(user.getEmail()).thenReturn("test@example.com");

        return subscription;
    }
}
