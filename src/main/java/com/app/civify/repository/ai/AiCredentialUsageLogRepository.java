package com.app.civify.repository.ai;

import com.app.civify.entity.ai.AiCredentialUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiCredentialUsageLogRepository extends JpaRepository<AiCredentialUsageLog, Long> {

    List<AiCredentialUsageLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime from, LocalDateTime to);
}
