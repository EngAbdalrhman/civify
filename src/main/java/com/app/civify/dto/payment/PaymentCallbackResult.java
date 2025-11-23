package com.app.civify.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackResult {
    private boolean success;
    private String transactionId;
    private String orderId;
    private String message;
    private String gatewayName;
}
