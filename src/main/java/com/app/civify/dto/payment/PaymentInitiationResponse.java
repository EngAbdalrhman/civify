package com.app.civify.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationResponse {
    private String paymentKey;
    private String orderId;
    private Long subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String paymentUrl; // URL to redirect user for payment
    private String message;
}
