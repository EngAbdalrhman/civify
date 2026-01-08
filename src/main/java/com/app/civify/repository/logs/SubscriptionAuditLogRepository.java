package com.app.civify.repository.logs;

import com.app.civify.entity.logs.SubscriptionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionAuditLogRepository extends JpaRepository<SubscriptionAuditLog, Long>,
                                                         JpaSpecificationExecutor<SubscriptionAuditLog> {
}
