package com.app.civify.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private Long id;
    private UUID uuid;
    private String planName;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;
    private String status;
    private String billingType;
    private String description;
    private String gateway;
    private String paymentUrl;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
