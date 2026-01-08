package com.app.civify.repository.logs;

import com.app.civify.entity.logs.BillingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingAuditLogRepository extends JpaRepository<BillingAuditLog, Long>,
                                                    JpaSpecificationExecutor<BillingAuditLog> {
}
