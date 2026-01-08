package com.app.civify.service.logs;

import com.app.civify.dto.token.TokenUsageDto;
import com.app.civify.dto.token.TokenUsageSummaryDto;
import com.app.civify.entity.enums.TokenSource;
import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.entity.logs.TokenUsageLog;
import com.app.civify.repository.logs.TokenUsageLogRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenUsageService extends CommonService {

    private final TokenUsageLogRepository tokenUsageLogRepository;

    /**
     * Log token usage event.
     */
    @Transactional
    public void logUsage(Long userId, TokenUsageType usageType, int tokensUsed,
                         int fromSubscription, int fromWallet, String description) {
        TokenSource source = determineSource(fromSubscription, fromWallet);

        TokenUsageLog logEntry = TokenUsageLog.builder()
                .userId(userId)
                .usageType(usageType)
                .source(source)
                .tokensUsed(tokensUsed)
                .fromSubscription(fromSubscription)
                .fromWallet(fromWallet)
                .description(description)
                .build();

        tokenUsageLogRepository.save(logEntry);
        log.info("Logged token usage: user={}, type={}, tokens={}, source={}",
                userId, usageType, tokensUsed, source);
    }

    /**
     * Get user's token usage history (simple pagination for user's own view).
     */
    public Page<TokenUsageDto> getUserUsageHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TokenUsageLog> logs = tokenUsageLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return logs.map(this::mapToDto);
    }

    /**
     * Get user's monthly token usage summary.
     */
    public TokenUsageSummaryDto getUserMonthlySummary(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = now.with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        int totalUsed = tokenUsageLogRepository.sumTokensByUserIdAndPeriod(userId, monthStart, monthEnd);
        int fromSub = tokenUsageLogRepository.sumFromSubscriptionByUserIdAndPeriod(userId, monthStart, monthEnd);
        int fromWallet = tokenUsageLogRepository.sumFromWalletByUserIdAndPeriod(userId, monthStart, monthEnd);

        return TokenUsageSummaryDto.builder()
                .totalTokensUsed(totalUsed)
                .fromSubscription(fromSub)
                .fromWallet(fromWallet)
                .periodStart(monthStart)
                .periodEnd(monthEnd)
                .build();
    }

    /**
     * Get all token usage logs with generic filter (for admin).
     */
    public PagedResponse<TokenUsageDto> filterUsageLogs(InputDataModel inputData) {
        Page<TokenUsageLog> page = filterEntities(tokenUsageLogRepository, inputData);
        Page<TokenUsageDto> dtoPage = page.map(this::mapToDto);
        return PagedResponse.of(dtoPage);
    }

    private TokenSource determineSource(int fromSubscription, int fromWallet) {
        if (fromSubscription > 0 && fromWallet > 0) {
            return TokenSource.MIXED;
        } else if (fromSubscription > 0) {
            return TokenSource.SUBSCRIPTION;
        } else {
            return TokenSource.WALLET;
        }
    }

    private TokenUsageDto mapToDto(TokenUsageLog logEntry) {
        return TokenUsageDto.builder()
                .id(logEntry.getId())
                .usageType(logEntry.getUsageType())
                .source(logEntry.getSource())
                .tokensUsed(logEntry.getTokensUsed())
                .fromSubscription(logEntry.getFromSubscription())
                .fromWallet(logEntry.getFromWallet())
                .description(logEntry.getDescription())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
