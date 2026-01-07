package com.app.civify.job;

import com.app.civify.entity.billing.Invoice;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.service.billing.InvoiceService;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Quartz job to handle all subscription lifecycle events:
 * - Invoice creation (3 days before expiration)
 * - Daily reminder emails until expiration
 * - Subscription expiration and downgrade
 * - Usage resets
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionJob implements Job {

    private static final String FREE_PLAN_NAME = "Free";
    private static final int DAYS_BEFORE_EXPIRY_FOR_INVOICE = 3;

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("⏰ Starting SubscriptionJob execution...");
        try {
            processInvoicingAndReminders();
            processExpiredSubscriptions();
            processUsageResets();
        } catch (Exception e) {
            log.error("❌ SubscriptionJob failed", e);
        }
        log.info("✅ SubscriptionJob execution finished.");
    }

    /**
     * Process invoicing and reminders for subscriptions expiring within 3 days.
     * - If no invoice exists: create one and send first email
     * - If invoice exists but unpaid: send daily reminder
     */
    private void processInvoicingAndReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(DAYS_BEFORE_EXPIRY_FOR_INVOICE);

        // Find active subscriptions expiring within the next 3 days (not expired yet)
        List<UserSubscription> expiringSubscriptions = subscriptionRepository.findRenewableSubscriptionsExpiringBetween(
                SubscriptionStatus.ACTIVE, now, threeDaysFromNow.plusDays(1).withHour(0).withMinute(0).withSecond(0)
        );

        int invoiceCount = 0;
        int reminderCount = 0;
        int skippedCount = 0;

        for (UserSubscription sub : expiringSubscriptions) {
            try {
                // Skip if user cancelled auto-renew
                if (Boolean.FALSE.equals(sub.getAutoRenew())) {
                    log.info("⏭️ Skipping subscription {} - autoRenew=false (user cancelled)", sub.getId());
                    skippedCount++;
                    continue;
                }

                SubscriptionPlan plan = sub.getPlan();

                // Skip free plan
                if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
                    continue;
                }

                // Skip if plan doesn't allow renewal
                if (Boolean.FALSE.equals(plan.getRenewalAllowed())) {
                    log.info("⏭️ Skipping subscription {} - plan doesn't allow renewal", sub.getId());
                    skippedCount++;
                    continue;
                }

                boolean isAnnual = isAnnualSubscription(sub);
                BigDecimal amount = isAnnual ? plan.getAnnualPrice() : plan.getMonthlyPrice();

                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("⚠️ Skipping subscription {} - no valid price", sub.getId());
                    continue;
                }

                // Check if PENDING invoice already exists
                Optional<Invoice> existingInvoice = invoiceService.findPendingInvoice(sub.getId());
                
                if (existingInvoice.isPresent()) {
                    // Invoice exists - send daily reminder (uses InvoiceService which has checkout link)
                    Invoice invoice = existingInvoice.get();
                    invoiceService.sendReminderEmail(invoice);
                    reminderCount++;
                    log.info("📧 Sent reminder for invoice {} (subscription {})", invoice.getUuid(), sub.getId());
                } else {
                    // No invoice - create one and send first email
                    Optional<Invoice> invoiceOpt = invoiceService.createRenewalInvoice(sub, amount, "EGP");

                    if (invoiceOpt.isPresent()) {
                        Invoice invoice = invoiceOpt.get();
                        sendRenewalNotificationEmail(sub, invoice, amount, isAnnual);
                        invoiceCount++;
                    }
                }
            } catch (Exception e) {
                log.error("❌ Error processing subscription {}: {}", sub.getId(), e.getMessage());
            }
        }

        log.info("📝 Created {} renewal invoices, sent {} reminders, skipped {} subscriptions.",
                invoiceCount, reminderCount, skippedCount);
    }

    /**
     * Determine if subscription is annual based on duration between start and end dates.
     */
    private boolean isAnnualSubscription(UserSubscription sub) {
        long daysBetween = ChronoUnit.DAYS.between(sub.getStartDate(), sub.getEndDate());
        return daysBetween > 180; // More than 6 months = annual
    }

    /**
     * Send first renewal email with checkout link when invoice is created.
     */
    private void sendRenewalNotificationEmail(UserSubscription sub, Invoice invoice, BigDecimal amount, boolean isAnnual) {
        try {
            String checkoutUrl = frontendUrl + "/checkout?invoice=" + invoice.getUuid();
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate());
            
            String subject = "Your Civify Subscription Renewal";
            String bodyContent = String.format("""
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>Your <strong>%s</strong> subscription to <strong>%s</strong> plan is expiring in <strong>%d days</strong>.</p>
                    
                    <div style="background-color: #e8f4f8; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0;"><strong>Renewal Amount:</strong> %.2f EGP</p>
                        <p style="margin: 5px 0 0 0;"><strong>Expiration Date:</strong> %s</p>
                    </div>
                    
                    <p>Please complete your payment to continue enjoying Civify services:</p>
                    
                    <p style="text-align: center; margin: 25px 0;">
                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Complete Payment</a>
                    </p>
                    
                    <p>If you do not renew before expiration, you will be automatically moved to the free plan.</p>
                    
                    <p>Thank you for being a valued customer!</p>
                    
                    <p>Best regards,<br>The Civify Team</p>
                    """,
                    sub.getUser().getName(),
                    isAnnual ? "annual" : "monthly",
                    sub.getPlan().getName(),
                    daysUntilExpiry,
                    amount.doubleValue(),
                    sub.getEndDate().toLocalDate().toString(),
                    checkoutUrl
            );

            emailService.sendHtmlEmailToUser(sub.getUser().getId(), sub.getUser().getEmail(), subject, bodyContent);
            log.info("📧 Sent renewal email for subscription {} to {}", sub.getId(), sub.getUser().getEmail());
        } catch (Exception e) {
            log.warn("⚠️ Failed to send renewal email for subscription {}: {}", sub.getId(), e.getMessage());
        }
    }

    /**
     * Process expired subscriptions:
     * - Find active subscriptions past their endDate
     * - Expire pending invoices
     * - Downgrade to free plan
     */
    private void processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();

        // Find all active subscriptions that have passed their endDate (including today)
        List<UserSubscription> expired = subscriptionRepository.findByStatusAndEndDateBefore(
                SubscriptionStatus.ACTIVE, now
        );

        int downgradedCount = 0;
        for (UserSubscription sub : expired) {
            try {
                // Skip free plan
                if (FREE_PLAN_NAME.equalsIgnoreCase(sub.getPlan().getName())) {
                    continue;
                }

                String reason = Boolean.FALSE.equals(sub.getAutoRenew())
                        ? "cancelled by user"
                        : "not renewed (payment not received)";

                log.info("📉 Downgrading subscription {} (User {}) - {} - expired on {}",
                        sub.getId(), sub.getUser().getId(), reason, sub.getEndDate());

                // Expire any pending invoices for this subscription
                invoiceService.expirePendingInvoices(sub.getId());

                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);
                subscriptionService.reactivateFreePlan(sub.getUser().getId());
                
                // Send expiration notification
                sendExpirationNotification(sub);
                
                downgradedCount++;
            } catch (Exception e) {
                log.error("❌ Error downgrading subscription {}", sub.getId(), e);
            }
        }

        if (downgradedCount > 0) {
            log.info("📉 Downgraded {} expired subscriptions.", downgradedCount);
        }
    }

    /**
     * Send notification when subscription expires and user is downgraded.
     */
    private void sendExpirationNotification(UserSubscription subscription) {
        try {
            String subject = "Subscription Expired - Downgraded to Free Plan";
            String bodyContent = String.format("""
                    <p>Dear <strong>%s</strong>,</p>
                    
                    <p>Your subscription to <strong>%s</strong> plan has expired.</p>
                    
                    <p>You have been automatically moved to the <strong>Free plan</strong>.</p>
                    
                    <div style="background-color: #e8f4f8; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0;"><strong>ℹ️ Want to continue?</strong> To restore your premium features, please subscribe again from your account.</p>
                    </div>
                    
                    <p>Best regards,<br>Civify Team</p>
                    """,
                    subscription.getUser().getUsername(),
                    subscription.getPlan().getName()
            );

            emailService.sendHtmlEmailToUser(
                    subscription.getUser().getId(),
                    subscription.getUser().getEmail(),
                    subject,
                    bodyContent
            );
        } catch (Exception e) {
            log.error("⚠️ Failed to send expiration notification: {}", e.getMessage());
        }
    }

    /**
     * Reset CVs and tokens if billing cycle month has passed.
     */
    private void processUsageResets() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> needsReset = subscriptionRepository.findByUsageResetDateBefore(now);

        for (UserSubscription sub : needsReset) {
            log.info("🔄 Resetting usage for subscription {} (User {})", sub.getId(), sub.getUser().getId());

            sub.setCurrentCvCount(0);
            sub.setCurrentTokenUsage(0);

            LocalDateTime nextDate = sub.getUsageResetDate().plusMonths(1);
            if (nextDate.isBefore(now)) {
                nextDate = now.plusMonths(1);
            }
            sub.setUsageResetDate(nextDate);

            subscriptionRepository.save(sub);
        }
    }
}
