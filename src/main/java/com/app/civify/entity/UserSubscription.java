package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class UserSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    // Current usage counters
    private Integer currentCvCount = 0;
    private Integer currentApiCount = 0;

    // Applied promo code (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    private String paymentTransactionId;

    public enum SubscriptionStatus {
        ACTIVE, EXPIRED, CANCELLED, PENDING
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
                && LocalDateTime.now().isBefore(endDate);
    }

    public boolean canUseCv() {
        return isActive() && (plan.getCvLimit() == null || currentCvCount < plan.getCvLimit());
    }

    public boolean canUseApi() {
        return isActive() && (plan.getApiLimit() == null || currentApiCount < plan.getApiLimit());
    }

    public void incrementCvCount() {
        if (currentCvCount == null) {
            currentCvCount = 0;
        }
        currentCvCount++;
    }

    public void incrementApiCount() {
        if (currentApiCount == null) {
            currentApiCount = 0;
        }
        currentApiCount++;
    }
}
