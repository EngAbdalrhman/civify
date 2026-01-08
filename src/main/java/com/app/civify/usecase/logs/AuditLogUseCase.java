package com.app.civify.usecase.logs;

import com.app.civify.dto.audit.BillingAuditDto;
import com.app.civify.dto.audit.SubscriptionAuditDto;
import com.app.civify.dto.token.TokenUsageDto;
import com.app.civify.dto.token.TokenUsageSummaryDto;
import com.app.civify.entity.User;
import com.app.civify.security.UserPrincipal;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import org.springframework.data.domain.Page;

public interface AuditLogUseCase {
    // Token Usage
    Page<TokenUsageDto> getMyTokenUsageHistory(UserPrincipal user, int page, int size);

    TokenUsageSummaryDto getMyTokenUsageSummary(UserPrincipal user);

    PagedResponse<TokenUsageDto> filterTokenUsageLogs(InputDataModel inputData);

    // Subscription Audit
    PagedResponse<SubscriptionAuditDto> filterSubscriptionAuditLogs(InputDataModel inputData);

    // Billing Audit
    PagedResponse<BillingAuditDto> filterBillingAuditLogs(InputDataModel inputData);
}
