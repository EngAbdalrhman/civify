package com.app.civify.dto.subscription;

import com.app.civify.entity.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for UserSubscription to avoid LazyInitializationException when serializing User entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionDto {
    private Long id;
    
    // Flattened User details
    private Long userId;
    private String userName;
    private String userEmail;
    
    // Flattened Plan details
    private Long planId;
    private String planName;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private SubscriptionStatus status;
    
    // Usage counters
    private Integer currentCvCount;
    private Integer currentApiCount;
    private Integer currentTokenUsage;
    private LocalDateTime usageResetDate;
    
    // Optional details
    private String promoCode;
    private String paymentTransactionId;
}
