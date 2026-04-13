package com.app.civify.repository;

import com.app.civify.entity.ScheduledEmailBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledEmailBatchRepository extends JpaRepository<ScheduledEmailBatch, Long> {

    /**
     * Find batches that are ready to be processed:
     * - Status is PENDING or FAILED
     * - Retry count is below max
     * - nextRetryAt is null (new) or in the past (due for retry)
     */
    @Query("SELECT b FROM ScheduledEmailBatch b WHERE " +
            "(b.status = 'PENDING' OR b.status = 'FAILED') " +
            "AND b.retryCount < b.maxRetries " +
            "AND (b.nextRetryAt IS NULL OR b.nextRetryAt <= :now) " +
            "ORDER BY b.createdAt ASC")
    List<ScheduledEmailBatch> findBatchesReadyToSend(@Param("now") LocalDateTime now);
}
