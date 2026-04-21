package com.app.civify.usecase.logs;

import com.app.civify.dto.audit.BillingAuditDto;
import com.app.civify.dto.audit.SubscriptionAuditDto;
import com.app.civify.dto.billing.BillingTransactionDto;
import com.app.civify.dto.token.TokenUsageDto;
import com.app.civify.dto.token.TokenUsageSummaryDto;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.logs.BillingAuditService;
import com.app.civify.service.logs.SubscriptionAuditService;
import com.app.civify.service.logs.TokenUsageService;
import com.app.civify.service.billing.BillingTransactionService;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogUseCaseImpl extends CommonService implements AuditLogUseCase {

    private final TokenUsageService tokenUsageService;
    private final SubscriptionAuditService subscriptionAuditService;
    private final BillingAuditService billingAuditService;
    private final BillingTransactionService billingTransactionService;

    @Override
    public Page<TokenUsageDto> getMyTokenUsageHistory(UserPrincipal user, int page, int size) {
        log.debug("UseCase: Getting token usage history for user: {}", user.getEmail());
        return tokenUsageService.getUserUsageHistory(user.getId(), page, size);
    }

    @Override
    public TokenUsageSummaryDto getMyTokenUsageSummary(UserPrincipal user) {
        log.debug("UseCase: Getting token usage summary for user: {}", user.getEmail());
        return tokenUsageService.getUserMonthlySummary(user.getId());
    }

    @Override
    public PagedResponse<TokenUsageDto> filterTokenUsageLogs(InputDataModel inputData) {
        log.debug("UseCase: Admin filtering token usage logs");
        return tokenUsageService.filterUsageLogs(inputData);
    }

    @Override
    public PagedResponse<SubscriptionAuditDto> filterSubscriptionAuditLogs(InputDataModel inputData) {
        log.debug("UseCase: Admin filtering subscription audit logs");
        return subscriptionAuditService.filterAuditLogs(inputData);
    }

    @Override
    public PagedResponse<BillingAuditDto> filterBillingAuditLogs(InputDataModel inputData) {
        log.debug("UseCase: Admin filtering billing audit logs");
        return billingAuditService.filterAuditLogs(inputData);
    }

    @Override
    public PagedResponse<BillingTransactionDto> filterBillingTransactions(
            InputDataModel inputData) {
        log.debug("UseCase: Admin filtering billing transactions");
        return billingTransactionService.filterTransactions(inputData);
    }
}
