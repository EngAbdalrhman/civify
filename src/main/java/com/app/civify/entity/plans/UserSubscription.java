package com.app.civify.entity.plans;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

import org.hibernate.annotations.Where;

@Entity
@Data
@Table(name = "user_subscriptions")
@EqualsAndHashCode(callSuper = true)
@Where(clause = "is_deleted = false")
public class UserSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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
    private Integer currentApiCount = 0;

    /**
     * Monthly token usage (for AI/LLM metering). Reset monthly alongside other counters.
     */
    @Column(nullable = false)
    private Integer currentTokenUsage = 0;

    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate;

    // Applied promo code (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    private String paymentTransactionId;

    /**
     * Check if subscription is currently active and not expired
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
                && LocalDateTime.now().isBefore(endDate);
    }

    /**
     * Check if user can generate more CVs within their limit
     */
    public boolean canUseCv() {
        if (!isActive()) {
            return false;
        }
        resetUsageIfNewMonth();
        return plan.getCvLimit() == null || currentCvCount < plan.getCvLimit();
    }

    /**
     * Check if user can make more API calls within their limit
     */
    public boolean canUseApi() {
        if (!isActive()) {
            return false;
        }
        resetUsageIfNewMonth();
        return plan.getApiLimit() == null || currentApiCount < plan.getApiLimit();
    }

    /**
     * Check if user has enough token balance for a request.
     */
    public boolean canUseTokens(int tokensNeeded) {
        if (!isActive()) {
            return false;
        }
        resetUsageIfNewMonth();
        if (plan.getTokenLimit() == null) {
            return true; // unlimited tokens
        }
        if (currentTokenUsage == null) currentTokenUsage = 0;
        return currentTokenUsage + tokensNeeded <= plan.getTokenLimit();
    }

    /**
     * Increment CV usage counter
     */
    public void incrementCvCount() {
        resetUsageIfNewMonth();
        if (currentCvCount == null) {
            currentCvCount = 0;
        }
        currentCvCount++;
    }

    /**
     * Increment API usage counter
     */
    public void incrementApiCount() {
        resetUsageIfNewMonth();
        if (currentApiCount == null) {
            currentApiCount = 0;
        }
        currentApiCount++;
    }

    /**
     * Charge tokens against the monthly allowance.
     */
    public void incrementTokenUsage(int tokensUsed) {
        resetUsageIfNewMonth();
        if (currentTokenUsage == null) {
            currentTokenUsage = 0;
        }
        currentTokenUsage += Math.max(tokensUsed, 0);
    }

    /**
     * Reset usage counters if it's a new month
     */
    public void resetUsageIfNewMonth() {
        LocalDateTime now = LocalDateTime.now();
        if (usageResetDate == null || now.isAfter(usageResetDate)) {
            currentCvCount = 0;
            currentApiCount = 0;
            currentTokenUsage = 0;
            // Set reset date to first day of next month
            usageResetDate = now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
    }

    // Expose user details for Admin API
    public String getUserName() {
        return user != null ? user.getName() : null;
    }

    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }
}
