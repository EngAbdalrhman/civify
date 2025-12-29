package com.app.civify.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBalanceResponse {
    private Integer walletTokens;        // User's purchased tokens (never expire)
    private Integer monthlyLimit;        // Plan's monthly token limit
    private Integer monthlyUsed;         // Tokens used this month from monthly allowance
    private Integer monthlyRemaining;    // Remaining from monthly allowance
    private Integer totalAvailable;      // Monthly remaining + wallet tokens
}
