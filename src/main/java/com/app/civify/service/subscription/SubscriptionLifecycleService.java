package com.app.civify.service.subscription;

import com.app.civify.entity.UserSubscription;
import com.app.civify.repository.UserSubscriptionRepository;
import com.app.civify.util.EmailService;
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
     * Cancels subscriptions that haven't been paid within 7 days of creation
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
     * Runs daily at 4 AM to check for subscriptions that need payment reminders
     * Sends reminders for pending subscriptions older than 3 days
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void processPaymentReminders() {
        log.info("Starting payment reminder processing");

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        List<UserSubscription> pendingSubscriptions = subscriptionRepository
                .findByStatusAndStartDateBefore(UserSubscription.SubscriptionStatus.PENDING, threeDaysAgo);

        for (UserSubscription subscription : pendingSubscriptions) {
            try {
                sendPaymentReminder(subscription);
                log.info("Sent payment reminder for user {}", subscription.getUser().getId());
            } catch (Exception e) {
                log.error("Error sending payment reminder for subscription {}: {}",
                        subscription.getId(), e.getMessage());
            }
        }

        log.info("Sent payment reminders for {} subscriptions", pendingSubscriptions.size());
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
        try {
            String subject = "Subscription Renewal Reminder";
            String body = String.format(
                    "Dear %s,\n\n"
                    + "Your subscription to %s plan is expiring on %s.\n"
                    + "Please renew your subscription to continue enjoying premium features.\n\n"
                    + "Best regards,\n"
                    + "Civify Team",
                    subscription.getUser().getUsername(),
                    subscription.getPlan().getName(),
                    subscription.getEndDate()
            );

            emailService.sendEmail(subscription.getUser().getEmail(), subject, body);
            log.info("Renewal reminder sent to user {} for plan {}",
                    subscription.getUser().getId(), subscription.getPlan().getName());
        } catch (Exception e) {
            log.error("Failed to send renewal reminder to user {}: {}",
                    subscription.getUser().getId(), e.getMessage());
        }
    }

    private void sendPaymentReminder(UserSubscription subscription) {
        try {
            String subject = "Payment Reminder - Complete Your Subscription";
            String body = String.format(
                    "Dear %s,\n\n"
                    + "You have a pending subscription to %s plan that was created on %s.\n"
                    + "Please complete your payment to activate your subscription and start enjoying premium features.\n\n"
                    + "If you have any questions, please contact our support team.\n\n"
                    + "Best regards,\n"
                    + "Civify Team",
                    subscription.getUser().getUsername(),
                    subscription.getPlan().getName(),
                    subscription.getStartDate().toLocalDate()
            );

            emailService.sendEmail(subscription.getUser().getEmail(), subject, body);
            log.info("Payment reminder sent to user {} for plan {}",
                    subscription.getUser().getId(), subscription.getPlan().getName());
        } catch (Exception e) {
            log.error("Failed to send payment reminder to user {}: {}",
                    subscription.getUser().getId(), e.getMessage());
        }
    }

    private void downgradeUserToFreePlan(Long userId) {
        subscriptionService.downgradeUserToFreePlan(userId);
    }
}
