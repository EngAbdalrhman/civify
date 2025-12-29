package com.app.civify.dto.plan;

import com.app.civify.entity.enums.PromoApplicabilityType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdatePromoCodeRequest {
    private String description;
    private BigDecimal discountValue;
    private BigDecimal minimumPurchase;
    private Integer usageLimit;
    private LocalDateTime expiresAt;
    private Boolean active;
    private PromoApplicabilityType applicabilityType;
    private BigDecimal maxDiscountAmount;
}
