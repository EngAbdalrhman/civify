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
     * Send email notification about upcoming renewal with payment link using centralized templates
     */
    private void sendRenewalNotificationEmail(UserSubscription sub, String paymentUrl, BigDecimal amount, boolean isAnnual) {
        try {
            String subject = "Your Civify Subscription Renewal";
            String bodyContent = String.format("""
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>Your <strong>%s</strong> subscription to <strong>%s</strong> plan is expiring soon.</p>
                    
                    <div style="background-color: #e8f4f8; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 0;"><strong>Renewal Amount:</strong> %.2f EGP</p>
                    </div>
                    
                    <p>Please complete your payment to continue enjoying Civify services:</p>
                    
                    <p style="text-align: center; margin: 25px 0;">
                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">Complete Payment</a>
                    </p>
                    
                    <p>Thank you for being a valued customer!</p>
                    
                    <p>Best regards,<br>The Civify Team</p>
                    """,
                    sub.getUser().getName(),
                    isAnnual ? "annual" : "monthly",
                    sub.getPlan().getName(),
                    amount.doubleValue(),
                    paymentUrl
            );
            
            emailService.sendHtmlEmailToUser(sub.getUser().getId(), sub.getUser().getEmail(), subject, bodyContent);
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
