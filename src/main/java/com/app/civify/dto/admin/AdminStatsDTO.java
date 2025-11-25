package com.app.civify.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
