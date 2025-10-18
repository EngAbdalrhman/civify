package com.app.civify.dto.payment.paymob;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymobOrderResponse {
    private Long id;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("delivery_needed")
    private Boolean deliveryNeeded;
    
    private String merchant;
    
    private String collector;
    
    @JsonProperty("amount_cents")
    private Integer amountCents;
    
    private String currency;
    
    @JsonProperty("is_payment_locked")
    private Boolean isPaymentLocked;
}
