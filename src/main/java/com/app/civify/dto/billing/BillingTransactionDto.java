package com.app.civify.dto.billing;

import com.app.civify.entity.enums.PaymentStatus;
import com.app.civify.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingTransactionDto {
    private String id;
    private Long userId;
    private String orderId;
    private TransactionType type;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String gateway;
    private Long subscriptionId;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
