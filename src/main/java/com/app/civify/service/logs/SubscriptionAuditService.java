package com.app.civify.service.logs;

import com.app.civify.dto.audit.SubscriptionAuditDto;
import com.app.civify.entity.enums.SubscriptionAuditType;
import com.app.civify.entity.logs.SubscriptionAuditLog;
import com.app.civify.repository.logs.SubscriptionAuditLogRepository;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;
import com.app.civify.util.persistence.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAuditService extends CommonService {

    private final SubscriptionAuditLogRepository auditLogRepository;

    /**
     * Log a subscription lifecycle event.
     */
    @Transactional
    public void logEvent(Long userId, Long subscriptionId, SubscriptionAuditType type,
                         Long invoiceId, Long planId, String performedBy, String details) {
        SubscriptionAuditLog logEntry = SubscriptionAuditLog.builder()
                .userId(userId)
                .subscriptionId(subscriptionId)
                .auditType(type)
                .invoiceId(invoiceId)
                .planId(planId)
                .performedBy(performedBy)
                .details(details)
                .build();

        auditLogRepository.save(logEntry);
        log.info("Logged subscription audit: user={}, type={}, subscription={}", 
                userId, type, subscriptionId);
    }

    /**
     * Get all subscription audit logs with generic filter (for admin).
     */
    public PagedResponse<SubscriptionAuditDto> filterAuditLogs(InputDataModel inputData) {
        Page<SubscriptionAuditLog> page = filterEntities(auditLogRepository, inputData);
        Page<SubscriptionAuditDto> dtoPage = page.map(this::mapToDto);
        return PagedResponse.of(dtoPage);
    }

    private SubscriptionAuditDto mapToDto(SubscriptionAuditLog logEntry) {
        return SubscriptionAuditDto.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .subscriptionId(logEntry.getSubscriptionId())
                .auditType(logEntry.getAuditType())
                .invoiceId(logEntry.getInvoiceId())
                .planId(logEntry.getPlanId())
                .performedBy(logEntry.getPerformedBy())
                .details(logEntry.getDetails())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
