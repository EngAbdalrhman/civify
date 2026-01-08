package com.app.civify.dto.audit;

import com.app.civify.entity.enums.BillingAuditType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingAuditDto {
    private Long id;
    private Long userId;
    private BillingAuditType auditType;
    private Long invoiceId;
    private String transactionId;
    private String gateway;
    private String gatewayInvoiceId;
    private String gatewayStatus;
    private String paymentMethod;
    private String result;
    private String ipAddress;
    private LocalDateTime createdAt;
}
