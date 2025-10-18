package com.app.civify.entity;

import com.app.civify.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Long userId;

    private String orderId;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private LocalDateTime completedAt;

    // Gateway information for multi-gateway support
    @Column(nullable = false)
    private String gateway;

    // Additional metadata (JSONB for PostgreSQL)
    @Column(columnDefinition = "jsonb")
    private String metadata;
}
