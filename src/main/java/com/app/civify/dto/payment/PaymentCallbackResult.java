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
    
    /**
     * Machine-readable error code for frontend i18n translation
     * Examples: WEBHOOK_SIGNATURE_INVALID, PAYMENT_COMPLETED, ALREADY_PROCESSED
     */
    private String errorCode;
    
    /**
     * Indicates if this webhook was already processed (idempotency check)
     */
    private boolean alreadyProcessed;
}

