package com.app.civify.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPerCvPurchaseRequest {
    private String resumeId;
    @Builder.Default
    private String productId = "PAY_PER_CV_SINGLE";
    private String gateway;
    @Builder.Default
    private PaymentMethods method = PaymentMethods.CARD;
    private String phoneNumber;
    private String promoCode;
}
