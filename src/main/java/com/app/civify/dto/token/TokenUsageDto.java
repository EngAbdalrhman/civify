package com.app.civify.dto.token;

import com.app.civify.entity.enums.TokenSource;
import com.app.civify.entity.enums.TokenUsageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageDto {
    private Long id;
    private TokenUsageType usageType;
    private TokenSource source;
    private int tokensUsed;
    private int fromSubscription;
    private int fromWallet;
    private String description;
    private LocalDateTime createdAt;
}
