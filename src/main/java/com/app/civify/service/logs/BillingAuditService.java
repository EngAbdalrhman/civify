package com.app.civify.service.logs;

import com.app.civify.dto.audit.BillingAuditDto;
import com.app.civify.entity.enums.BillingAuditType;
import com.app.civify.entity.logs.BillingAuditLog;
import com.app.civify.repository.logs.BillingAuditLogRepository;
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
public class BillingAuditService extends CommonService {

    private final BillingAuditLogRepository auditLogRepository;

    /**
     * Log a webhook event from payment gateway.
     */
    @Transactional
    public void logWebhook(String gateway, Long userId, Long invoiceId, String transactionId,
                           String gatewayInvoiceId, String gatewayStatus, String paymentMethod,
                           String result, String webhookPayload, String ipAddress,
                           BillingAuditType auditType) {
        BillingAuditLog logEntry = BillingAuditLog.builder()
                .userId(userId)
                .auditType(auditType)
                .invoiceId(invoiceId)
                .transactionId(transactionId)
                .gateway(gateway)
                .gatewayInvoiceId(gatewayInvoiceId)
                .gatewayStatus(gatewayStatus)
                .paymentMethod(paymentMethod)
                .result(result)
                .webhookPayload(webhookPayload)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(logEntry);
        log.info("Logged billing audit: type={}, gateway={}, invoiceId={}, txn={}",
                auditType, gateway, invoiceId, transactionId);
    }

    /**
     * Log an invoice action (cancel, expire, etc.).
     */
    @Transactional
    public void logInvoiceAction(Long invoiceId, Long userId, BillingAuditType auditType, String result) {
        BillingAuditLog logEntry = BillingAuditLog.builder()
                .userId(userId)
                .auditType(auditType)
                .invoiceId(invoiceId)
                .result(result)
                .build();

        auditLogRepository.save(logEntry);
        log.info("Logged invoice action: type={}, invoiceId={}, user={}",
                auditType, invoiceId, userId);
    }

    /**
     * Get all billing audit logs with generic filter (for admin).
     */
    public PagedResponse<BillingAuditDto> filterAuditLogs(InputDataModel inputData) {
        Page<BillingAuditLog> page = filterEntities(auditLogRepository, inputData);
        Page<BillingAuditDto> dtoPage = page.map(this::mapToDto);
        return PagedResponse.of(dtoPage);
    }

    private BillingAuditDto mapToDto(BillingAuditLog logEntry) {
        return BillingAuditDto.builder()
                .id(logEntry.getId())
                .userId(logEntry.getUserId())
                .auditType(logEntry.getAuditType())
                .invoiceId(logEntry.getInvoiceId())
                .transactionId(logEntry.getTransactionId())
                .gateway(logEntry.getGateway())
                .gatewayInvoiceId(logEntry.getGatewayInvoiceId())
                .gatewayStatus(logEntry.getGatewayStatus())
                .paymentMethod(logEntry.getPaymentMethod())
                .result(logEntry.getResult())
                .ipAddress(logEntry.getIpAddress())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
