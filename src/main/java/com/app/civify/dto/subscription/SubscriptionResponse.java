package com.app.civify.dto.subscription;

import com.app.civify.entity.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private Long planId;
    private String planName;
    private String planDescription;
    private Integer cvLimit;
    private Integer apiLimit;
    private Integer currentCvCount;
    private Integer currentApiCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private SubscriptionStatus status;
    private String paymentTransactionId;
}
