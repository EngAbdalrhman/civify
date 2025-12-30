package com.app.civify.dto.plan;

import com.app.civify.entity.enums.PromoApplicabilityType;
import com.app.civify.entity.enums.DiscountType;
import com.app.civify.util.security.EncodedIdField;
import com.app.civify.util.security.EncodedIdSerializer;
import com.app.civify.util.security.EntityType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromoCodeDto {
    @JsonSerialize(using = EncodedIdSerializer.class)
    @EncodedIdField(EntityType.PROMO_CODE)
    private Long id;
    
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumPurchase;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private boolean active;
    private PromoApplicabilityType applicabilityType;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

