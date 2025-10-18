package com.app.civify.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Long planId;
    private String promoCode;
    private boolean isAnnual;
    private String gateway; // Optional: specify payment gateway (defaults to "paymob")
}
