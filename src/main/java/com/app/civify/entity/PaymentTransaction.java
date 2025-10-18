package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    private String status;

    private double amount;

    private LocalDateTime completedAt;

    // Gateway information for multi-gateway support
    private String gateway;

    // Additional metadata (JSON string)
    @Column(length = 2000)
    private String metadata;
}
