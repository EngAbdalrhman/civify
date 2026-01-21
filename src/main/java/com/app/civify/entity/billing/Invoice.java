package com.app.civify.entity.billing;

import com.app.civify.dto.payment.PaymentMethods;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.InvoiceStatus;
import com.app.civify.entity.plans.PromoCode;
import com.app.civify.entity.plans.SubscriptionPlan;
import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Data
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("is_deleted = false")
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscription_id")
    private UserSubscription subscription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(length = 10)
    @ColumnDefault("'EGP'")
    private String currency = "EGP";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(length = 50)
    private String billingType;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String gateway;

    @Column(columnDefinition = "TEXT")
    private String paymentUrl;

    @Column
    @Enumerated(EnumType.STRING)
    private PaymentMethods paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String paymentTransactionId;

    private LocalDateTime dueDate;

    private LocalDateTime paidAt;

    private LocalDateTime expiredAt;

    private Boolean isAnnual = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean reminderSent = false;

    @PrePersist
    @Override
    public void prePersist() {
        super.prePersist();
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (status == null) {
            status = InvoiceStatus.PENDING;
        }
        if (currency == null) {
            currency = "EGP";
        }
    }

    public void markPaid(String transactionId) {
        this.status = InvoiceStatus.PAID;
        this.paymentTransactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }

    public void markExpired() {
        this.status = InvoiceStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = InvoiceStatus.CANCELLED;
    }

    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getUserName() {
        return user != null ? user.getName() : null;
    }

    public String getPlanName() {
        return plan != null ? plan.getName() : null;
    }
}
