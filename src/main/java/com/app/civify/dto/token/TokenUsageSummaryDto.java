package com.app.civify.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageSummaryDto {
    private int totalTokensUsed;
    private int fromSubscription;
    private int fromWallet;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
