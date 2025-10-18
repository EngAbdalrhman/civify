package com.app.civify.service.subscription;

import com.app.civify.entity.UserSubscription;
import com.app.civify.entity.enums.SubscriptionStatus;
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

/**
 * Service to handle subscription lifecycle events automatically
 * - Expire subscriptions when end date passes
 * - Cancel overdue pending payments (7 days grace period)
 * - Send payment and renewal reminders
 * - Auto-reactivate free plan when paid subscription expires
 */
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
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now);

        for (UserSubscription subscription : expiredSubscriptions) {
            try {
                expireSubscription(subscription);
                log.info("Expired subscription {} for user {}", subscription.getId(), subscription.getUser().getId());
            } catch (Exception e) {
                log.error("Error expiring subscription {}: {}", subscription.getId(), e.getMessage(), e);
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
                .findByStatusAndStartDateBefore(SubscriptionStatus.PENDING, oneWeekAgo);

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
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // Find pending subscriptions between 3-7 days old (not yet overdue)
        List<UserSubscription> pendingSubscriptions = subscriptionRepository
                .findByStatusAndStartDateBetween(SubscriptionStatus.PENDING, sevenDaysAgo, threeDaysAgo);

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
     * Runs daily at 5 AM to send renewal reminders
     * Reminds users whose subscriptions are expiring in 3 days
     */
    @Scheduled(cron = "0 0 5 * * ?")
    @Transactional
    public void sendRenewalReminders() {
        log.info("Starting renewal reminder processing");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);

        List<UserSubscription> subscriptionsNeedingReminder = subscriptionRepository
                .findByStatusAndEndDateBetween(SubscriptionStatus.ACTIVE, now, threeDaysFromNow);

        for (UserSubscription subscription : subscriptionsNeedingReminder) {
            try {
                // Don't send reminders for free plan (it never expires)
                if (!"free".equalsIgnoreCase(subscription.getPlan().getName())) {
                    sendRenewalReminder(subscription);
                    log.info("Sent renewal reminder for user {}", subscription.getUser().getId());
                }
            } catch (Exception e) {
                log.error("Error sending renewal reminder for subscription {}: {}",
                        subscription.getId(), e.getMessage(), e);
            }
        }

        log.info("Sent renewal reminders for {} subscriptions", subscriptionsNeedingReminder.size());
    }

    /**
     * Expire an active subscription and reactivate free plan
     */
    private void expireSubscription(UserSubscription subscription) {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        // Reactivate free plan (Option A flow)
        subscriptionService.reactivateFreePlan(subscription.getUser().getId());

        // Send expiration notification
        try {
            sendExpirationNotification(subscription);
        } catch (Exception e) {
            log.error("Failed to send expiration notification: {}", e.getMessage());
        }
    }

    /**
     * Cancel an overdue pending subscription
     */
    private void cancelOverdueSubscription(UserSubscription subscription) {
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        // Send cancellation notification
        try {
            String subject = "Subscription Request Cancelled";
            String body = String.format(
                    "Dear %s,\n\n"
                    + "Your subscription request for %s plan has been cancelled due to non-payment.\n"
                    + "You can create a new subscription request anytime from your account.\n\n"
                    + "Best regards,\n"
                    + "Civify Team",
                    subscription.getUser().getUsername(),
                    subscription.getPlan().getName()
            );

            emailService.sendEmail(subscription.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    /**
     * Send renewal reminder email
     */
    private void sendRenewalReminder(UserSubscription subscription) {
        String subject = "Subscription Renewal Reminder";
        String body = String.format(
                "Dear %s,\n\n"
                + "Your subscription to %s plan is expiring on %s.\n"
                + "To continue enjoying premium features, please renew your subscription before it expires.\n\n"
                + "If your subscription expires, you will automatically be moved to the free plan.\n\n"
                + "Best regards,\n"
                + "Civify Team",
                subscription.getUser().getUsername(),
                subscription.getPlan().getName(),
                subscription.getEndDate().toLocalDate()
        );

        emailService.sendEmail(subscription.getUser().getEmail(), subject, body);
    }

    /**
     * Send payment reminder email
     */
    private void sendPaymentReminder(UserSubscription subscription) {
        String subject = "Payment Reminder - Complete Your Subscription";
        String body = String.format(
                "Dear %s,\n\n"
                + "You have a pending subscription to %s plan that was created on %s.\n"
                + "Please complete your payment to activate your subscription and start enjoying premium features.\n\n"
                + "If payment is not completed within 7 days, your subscription request will be automatically cancelled.\n\n"
                + "If you have any questions, please contact our support team.\n\n"
                + "Best regards,\n"
                + "Civify Team",
                subscription.getUser().getUsername(),
                subscription.getPlan().getName(),
                subscription.getStartDate().toLocalDate()
        );

        emailService.sendEmail(subscription.getUser().getEmail(), subject, body);
    }

    /**
     * Send subscription expiration notification
     */
    private void sendExpirationNotification(UserSubscription subscription) {
        String subject = "Subscription Expired";
        String body = String.format(
                "Dear %s,\n\n"
                + "Your subscription to %s plan has expired.\n"
                + "You have been automatically moved to the free plan.\n\n"
                + "To continue enjoying premium features, please renew your subscription from your account.\n\n"
                + "Best regards,\n"
                + "Civify Team",
                subscription.getUser().getUsername(),
                subscription.getPlan().getName()
        );

        emailService.sendEmail(subscription.getUser().getEmail(), subject, body);
    }
}
