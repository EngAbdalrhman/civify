package com.app.civify.entity.billing;

import com.app.civify.entity.utils.BaseEntity;
import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.entity.enums.TransactionType;
import com.app.civify.entity.plans.TokenPackage;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "billing_transactions")
public class BillingTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Long userId;

    private String orderId;

    @Column(columnDefinition = "TEXT")
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "transaction_type")
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "EGP";

    private LocalDateTime completedAt;

    // Gateway information for multi-gateway support
    @Column(nullable = false)
    private String gateway;

    // Link to subscription
    private Long subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_package_id")
    private TokenPackage tokenPackage;

    /** Card token used for this transaction (for auto-renewals) */
    @Column(name = "saved_card_token", length = 512)
    private String savedCardToken;

    // Additional metadata (JSONB for PostgreSQL)
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;

    @Version
    private Long version;
}
