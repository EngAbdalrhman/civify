package com.app.civify.dto.subscription;

import lombok.Data;

@Data
public class SubscriptionRequest {
    private Long planId;
    private String promoCode;
    private boolean isAnnual;
    private String currency;
}
