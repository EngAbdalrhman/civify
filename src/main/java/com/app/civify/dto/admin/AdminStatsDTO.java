package com.app.civify.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private long totalUsers;
    private long subscribedUsers;
    private long totalCVs;
    private long totalApiCalls;
    private long apiCallsThisMonth;
    private long activeSubscriptions;
    private BigDecimal periodRevenue;
    private BigDecimal revenueThisMonth;
    private BigDecimal totalRevenue;
    
    private BigDecimal subscriptionRevenue;
    private BigDecimal tokenPurchaseRevenue;
    private long completedTokenPurchases;
    
    // Period information
    private String period;      // MONTHLY, QUARTERLY, YEARLY, OVERALL
    private String periodStart; // ISO date string
    private String periodEnd;   // ISO date string
}

