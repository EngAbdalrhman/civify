package com.app.civify.service.subscription;

import com.app.civify.entity.UserSubscription;
import com.app.civify.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class SubscriptionLifecycleService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;

    /**
     * Runs daily at 2 AM to check for expired subscriptions
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processExpiredSubscriptions() {
        log.info("Starting expired subscription processing");

        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(UserSubscription.SubscriptionStatus.ACTIVE, now);

        for (UserSubscription subscription : expiredSubscriptions) {
            try {
                cancelExpiredSubscription(subscription);
                log.info("Cancelled expired subscription for user {}", subscription.getUser().getId());
            } catch (Exception e) {
                log.error("Error cancelling subscription {}: {}", subscription.getId(), e.getMessage());
            }
        }

        log.info("Processed {} expired subscriptions", expiredSubscriptions.size());
    }

    /**
     * Runs daily at 3 AM to check for subscriptions with overdue payments
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void processOverduePayments() {
        log.info("Starting overdue payment processing");

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        List<UserSubscription> overdueSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(UserSubscription.SubscriptionStatus.PENDING, oneWeekAgo);

        for (UserSubscription subscription : overdueSubscriptions) {
            try {
                cancelOverdueSubscription(subscription);
                log.info("Cancelled overdue subscription for user {}", subscription.getUser().getId());
            } catch (Exception e) {
                log.error("Error cancelling overdue subscription {}: {}", subscription.getId(), e.getMessage());
            }
        }

        log.info("Processed {} overdue subscriptions", overdueSubscriptions.size());
    }

    /**
     * Runs daily at 4 AM to send renewal reminders
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void sendRenewalReminders() {
        log.info("Starting renewal reminder processing");

        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        List<UserSubscription> subscriptionsNeedingReminder = subscriptionRepository
                .findByStatusAndEndDateBetween(UserSubscription.SubscriptionStatus.ACTIVE,
                        LocalDateTime.now(), threeDaysFromNow);

        for (UserSubscription subscription : subscriptionsNeedingReminder) {
            try {
                sendRenewalReminder(subscription);
                log.info("Sent renewal reminder for user {}", subscription.getUser().getId());
            } catch (Exception e) {
                log.error("Error sending renewal reminder for subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }
        }

        log.info("Sent renewal reminders for {} subscriptions", subscriptionsNeedingReminder.size());
    }

    private void cancelExpiredSubscription(UserSubscription subscription) {
        subscription.setStatus(UserSubscription.SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        // Downgrade user to free plan (ROLE_USER)
        downgradeUserToFreePlan(subscription.getUser().getId());
    }

    private void cancelOverdueSubscription(UserSubscription subscription) {
        subscription.setStatus(UserSubscription.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        // Downgrade user to free plan (ROLE_USER)
        downgradeUserToFreePlan(subscription.getUser().getId());
    }

    private void sendRenewalReminder(UserSubscription subscription) {
        // TODO: Implement email notification service
        log.info("Renewal reminder would be sent to user {} for plan {}",
                subscription.getUser().getId(), subscription.getPlan().getName());
    }

    private void downgradeUserToFreePlan(Long userId) {
        // TODO: Implement user role downgrade logic
        log.info("Downgrading user {} to free plan", userId);
    }
}
