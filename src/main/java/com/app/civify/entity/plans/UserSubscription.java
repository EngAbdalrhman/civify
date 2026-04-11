package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Data
@Table(name = "user_subscriptions")
@EqualsAndHashCode(callSuper = true)
@SQLRestriction(value = "is_deleted = false")
public class UserSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    // Current usage counters (reset monthly)
    @Column(nullable = false)
    private Integer currentCvCount = 0;

    @Column(nullable = false)
    private Integer currentJobMatchCount = 0;

    /**
     * Monthly token usage (for AI/LLM metering). Reset monthly alongside other
     * counters.
     */
    @Column(nullable = false)
    private Integer currentTokenUsage = 0;

    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(columnDefinition = "TEXT")
    private String paymentTransactionId;

    /**
     * Whether subscription should auto-renew when it expires. Set to false when
     * user cancels - subscription stays active until endDate.
     */
    @Column(name = "auto_renew", nullable = false)
    @ColumnDefault("true")
    private Boolean autoRenew = true;

    /**
     * Current billing cycle count (starts at 1). Incremented on each successful
     * renewal by Quartz scheduler.
     */
    @Column(name = "current_cycle_count", nullable = false)
    @ColumnDefault("1")
    private Integer currentCycleCount = 1;

    /**
     * Paymob saved card token for auto-renewal charges
     */
    @Column(name = "saved_card_token", length = 512)
    private String savedCardToken;

    /**
     * Masked PAN for display (e.g., "**** 1234")
     */
    @Column(name = "card_masked_pan", length = 30)
    private String cardMaskedPan;

    /**
     * Card network (e.g., "Visa", "MasterCard")
     */
    @Column(name = "card_type", length = 20)
    private String cardType;

    /**
     * Last auto-charge attempt timestamp
     */
    @Column(name = "last_auto_charge_attempt")
    private LocalDateTime lastAutoChargeAttempt;

    /**
     * Count of consecutive auto-charge failures
     */
    @Column(name = "auto_charge_failure_count")
    @ColumnDefault("0")
    private Integer autoChargeFailureCount = 0;

    /**
     * Check if subscription is currently active and not expired
     */
    public boolean isActive() {
        // Allow 3-day grace period for payment processing/retries
        // The scheduler downgrades after 2 days, so this ensures service continuity 
        // until the scheduler actually marks it EXPIRED.
        return status == SubscriptionStatus.ACTIVE
                && LocalDateTime.now().isBefore(endDate.plusDays(3));
    }

    /**
     * Check if user can generate more CVs within their limit
     */
    public boolean canUseCv() {
        if (!isActive()) {
            return false;
        }
        return plan.getCvLimit() == null || currentCvCount < plan.getCvLimit();
    }

    public void incrementJobMatchCount() {
        if (currentJobMatchCount == null) {
            currentJobMatchCount = 0;
        }
        this.currentJobMatchCount++;
    }

    /**
     * Check if user can perform more job match analyses within their limit
     */
    public boolean canUseJobMatch() {
        if (!isActive()) {
            return false;
        }
        return plan.getJobMatchLimit() == null || currentJobMatchCount < plan.getJobMatchLimit();
    }

    /**
     * Check if user has enough token balance from subscription plan only. Note:
     * This does NOT include wallet tokens (User.extraTokens). For complete
     * check including wallet, use canUseTokensWithWallet().
     */
    public boolean canUseTokens(int tokensNeeded) {
        if (!isActive()) {
            return false;
        }
        if (plan.getTokenLimit() == null) {
            return true; // unlimited tokens
        }
        if (currentTokenUsage == null) {
            currentTokenUsage = 0;
        }

        int monthlyRemaining = Math.max(0, plan.getTokenLimit() - currentTokenUsage);
        return monthlyRemaining >= tokensNeeded;
    }

    /**
     * Check if user has enough tokens including User's wallet (purchased
     * tokens). Tokens are deducted: first from subscription plan allowance,
     * then from wallet.
     *
     * @param tokensNeeded tokens required for the operation
     * @param userWalletTokens tokens from User.extraTokens (purchased tokens
     * that never expire)
     * @return true if total available tokens (subscription + wallet) is
     * sufficient
     */
    public boolean canUseTokensWithWallet(int tokensNeeded, int userWalletTokens) {
        if (!isActive()) {
            return false;
        }
        if (plan.getTokenLimit() == null) {
            return true; // unlimited tokens in subscription plan
        }
        if (currentTokenUsage == null) {
            currentTokenUsage = 0;
        }

        // Calculate remaining subscription tokens
        int monthlyRemaining = Math.max(0, plan.getTokenLimit() - currentTokenUsage);
        // Total = subscription remaining + wallet tokens
        int totalAvailable = monthlyRemaining + userWalletTokens;

        return totalAvailable >= tokensNeeded;
    }

    /**
     * Increment CV usage counter
     */
    public void incrementCvCount() {
        if (currentCvCount == null) {
            currentCvCount = 0;
        }
        currentCvCount++;
    }

    /**
     * Get remaining tokens from subscription plan allowance. Does NOT include
     * wallet tokens (User.extraTokens).
     *
     * @return remaining tokens in current billing period
     */
    public int getRemainingSubscriptionTokens() {
        if (plan.getTokenLimit() == null) {
            return Integer.MAX_VALUE; // unlimited
        }
        if (currentTokenUsage == null) {
            currentTokenUsage = 0;
        }
        return Math.max(0, plan.getTokenLimit() - currentTokenUsage);
    }

    /**
     * Reset usage counters for a new billing period. Called by Quartz scheduler
     * during subscription renewal. Sets usageResetDate to first day of next
     * month.
     */
    public void resetUsageCounters() {
        LocalDateTime now = LocalDateTime.now();
        currentCvCount = 0;
        currentJobMatchCount = 0;
        currentTokenUsage = 0;
        usageResetDate = now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * Check if usage should be reset (past the reset date). Used by Quartz
     * scheduler to determine when to reset.
     */
    public boolean shouldResetUsage() {
        return usageResetDate == null || LocalDateTime.now().isAfter(usageResetDate);
    }

    // Expose user details for Admin API
    public String getUserName() {
        return user != null ? user.getName() : null;
    }

    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }
}
