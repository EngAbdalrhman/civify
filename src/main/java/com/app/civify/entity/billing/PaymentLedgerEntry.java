package com.app.civify.entity.billing;

import com.app.civify.entity.enums.PaymentLedgerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_ledger", indexes = {
        @Index(name = "idx_payment_ledger_txn", columnList = "transaction_id"),
        @Index(name = "idx_payment_ledger_order", columnList = "order_id"),
        @Index(name = "idx_payment_ledger_status", columnList = "status"),
        @Index(name = "idx_payment_ledger_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;

    @Column(name = "order_id", length = 255)
    private String orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String gateway;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentLedgerStatus status;

    @Column(name = "gateway_status", length = 100)
    private String gatewayStatus;

    @Column(length = 50)
    private String source;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
