package com.app.civify.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoValidationResponse {
    private boolean valid;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String message;
}
