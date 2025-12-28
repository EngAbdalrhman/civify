package com.app.civify.job;

import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.app.civify.repository.plans.UserSubscriptionRepository;
import com.app.civify.service.subscription.SubscriptionService;
import com.app.civify.service.payment.PaymentService;
import com.app.civify.dto.payment.PaymentCreationResult;
import com.app.civify.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionJob implements Job {

    private static final String FREE_PLAN_NAME = "Free";

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final EmailService emailService;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("⏰ Starting SubscriptionJob execution...");
        try {
            processInvoicing();
            processExpiredSubscriptions();
            processUsageResets();
        } catch (Exception e) {
            log.error("❌ SubscriptionJob failed", e);
        }
        log.info("✅ SubscriptionJob execution finished.");
    }

    /**
     * Step 1: Invoicing
     * Create renewal invoices for subscriptions approaching their renewal date.
     * Skip if autoRenew is false (user cancelled).
     */
    private void processInvoicing() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowStart = now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrowEnd = now.plusDays(1).withHour(23).withMinute(59).withSecond(59);

        // Find renewable subscriptions expiring tomorrow
        List<UserSubscription> expiringTomorrow = subscriptionRepository.findRenewableSubscriptionsExpiringBetween(
                SubscriptionStatus.ACTIVE, tomorrowStart, tomorrowEnd
        );

        int invoiceCount = 0;
        int skippedCount = 0;
        for (UserSubscription sub : expiringTomorrow) {
            try {
                if (Boolean.FALSE.equals(sub.getAutoRenew())) {
                    log.info("⏭️ Skipping invoice for subscription {} - autoRenew=false (user cancelled)", sub.getId());
                    skippedCount++;
                    continue;
                }

                SubscriptionPlan plan = sub.getPlan();
                
                // Skip free plan
                if (FREE_PLAN_NAME.equalsIgnoreCase(plan.getName())) {
                    continue;
                }

                boolean isAnnual = isAnnualSubscription(sub);
                BigDecimal amount = isAnnual ? plan.getAnnualPrice() : plan.getMonthlyPrice();
                
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Skipping subscription {} - no valid price", sub.getId());
                    continue;
                }

                // Create renewal invoice via payment service
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("subscriptionId", sub.getId());
                metadata.put("renewalType", isAnnual ? "annual" : "monthly");
                metadata.put("firstName", sub.getUser().getName());
                metadata.put("email", sub.getUser().getEmail());

                PaymentCreationResult result = paymentService.createPayment(
                        sub.getUser().getId(),
                        amount.doubleValue(),
                        "fawaterak"
                );

                log.info("📧 Created renewal invoice for subscription {} (User {}). Payment URL: {}", 
                        sub.getId(), sub.getUser().getId(), result.getPaymentUrl());

                // Send renewal notification email
                sendRenewalNotificationEmail(sub, result.getPaymentUrl(), amount, isAnnual);
                
                invoiceCount++;
            } catch (Exception e) {
                log.error("Error creating invoice for subscription {}: {}", sub.getId(), e.getMessage());
            }
        }
        log.info("📝 Created {} renewal invoices, skipped {} cancelled subscriptions.", invoiceCount, skippedCount);
    }

    /**
     * Determine if subscription is annual based on duration between start and end dates.
     */
    private boolean isAnnualSubscription(UserSubscription sub) {
        // TODO check
        long daysBetween = ChronoUnit.DAYS.between(sub.getStartDate(), sub.getEndDate());
        return daysBetween > 180; // More than 6 months = annual
    }

    /**
     * Send email notification about upcoming renewal with payment link.
     */
    private void sendRenewalNotificationEmail(UserSubscription sub, String paymentUrl, BigDecimal amount, boolean isAnnual) {
        try {
            // TODO send HTML mail
            String subject = "Your Civify Subscription Renewal";
            String body = String.format(
                    "Hello %s,\n\n" +
                    "Your %s subscription to %s plan is expiring soon.\n\n" +
                    "Renewal amount: %.2f EGP\n\n" +
                    "Please complete your payment to continue enjoying Civify services:\n%s\n\n" +
                    "Thank you for being a valued customer!\n\n" +
                    "Best regards,\nThe Civify Team",
                    sub.getUser().getName(),
                    isAnnual ? "annual" : "monthly",
                    sub.getPlan().getName(),
                    amount.doubleValue(),
                    paymentUrl
            );
            emailService.sendEmail(sub.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send renewal email for subscription {}: {}", sub.getId(), e.getMessage());
        }
    }

    /**
     * Step 2: Process Expired Subscriptions
     * - Cancelled (autoRenew=false): Downgrade immediately at endDate
     * - All others that passed endDate: Also downgrade (no grace period per user request)
     */
    private void processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find all active subscriptions that have passed their endDate
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
                
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);
                subscriptionService.reactivateFreePlan(sub.getUser().getId());
                downgradedCount++;
            } catch (Exception e) {
                log.error("Error downgrading subscription {}", sub.getId(), e);
            }
        }
        
        if (downgradedCount > 0) {
            log.info("📉 Downgraded {} expired subscriptions.", downgradedCount);
        }
    }

    /**
     * Step 3: Usage Resets
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
