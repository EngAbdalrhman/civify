package com.app.civify.dto.plan;

import com.app.civify.entity.enums.PromoApplicabilityType;
import com.app.civify.entity.enums.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreatePromoCodeRequest {
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumPurchase;
    private Integer usageLimit;
    private LocalDateTime expiresAt;
    private PromoApplicabilityType applicabilityType;
    private BigDecimal maxDiscountAmount;
    private boolean active = true;
}
