package com.app.civify.usecase.logs;

import com.app.civify.dto.audit.BillingAuditDto;
import com.app.civify.dto.audit.SubscriptionAuditDto;
import com.app.civify.dto.token.TokenUsageDto;
import com.app.civify.dto.token.TokenUsageSummaryDto;
import com.app.civify.entity.User;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import org.springframework.data.domain.Page;

public interface AuditLogUseCase {
    // Token Usage
    Page<TokenUsageDto> getMyTokenUsageHistory(User user, int page, int size);
    TokenUsageSummaryDto getMyTokenUsageSummary(User user);
    PagedResponse<TokenUsageDto> filterTokenUsageLogs(InputDataModel inputData);
    Page<TokenUsageDto> getUserTokenUsageHistory(Long userId, int page, int size);
    
    // Subscription Audit
    PagedResponse<SubscriptionAuditDto> filterSubscriptionAuditLogs(InputDataModel inputData);
    
    // Billing Audit
    PagedResponse<BillingAuditDto> filterBillingAuditLogs(InputDataModel inputData);
}
