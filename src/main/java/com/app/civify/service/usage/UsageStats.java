package com.app.civify.service.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Usage statistics for a user's current subscription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStats {
    
    // Subscription info
    private Long subscriptionId;
    private String planName;
    private LocalDateTime subscriptionEndDate;
    
    // UI CV generation usage
    private Integer uiCvCount;
    private Integer uiCvLimit; // null = unlimited
    private Integer uiCvRemaining;
    
    // Stored CVs
    private Long storedCvCount;
    private Integer storedCvLimit; // null = unlimited
    private Long storedCvRemaining;
    
    // Reset info
    private LocalDateTime usageResetDate;
    
    // Helper methods
    public boolean isUiCvLimitReached() {
        return uiCvLimit != null && uiCvCount >= uiCvLimit;
    }
    
    public boolean isStoredCvLimitReached() {
        return storedCvLimit != null && storedCvCount >= storedCvLimit;
    }
}
