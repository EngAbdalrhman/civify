package com.app.civify.dto.subscription;

import com.app.civify.entity.enums.SubscriptionStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private String planName;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer currentCvCount;
    private Integer currentApiCount;
    private Integer currentTokenUsage;
    private LocalDateTime usageResetDate;
    private String promoCode;
    private String paymentTransactionId;
}
