package com.app.civify.dto.audit;

import com.app.civify.entity.enums.SubscriptionAuditType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionAuditDto {
    private Long id;
    private Long userId;
    private Long subscriptionId;
    private SubscriptionAuditType auditType;
    private Long invoiceId;
    private Long planId;
    private String performedBy;
    private String details;
    private LocalDateTime createdAt;
}
