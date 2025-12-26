package com.app.civify.repository.logs;

import com.app.civify.entity.logs.CvOperationLog;
import com.app.civify.entity.enums.CvOperationAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for CV operation logs.
 */
@Repository
public interface CvOperationLogRepository extends JpaRepository<CvOperationLog, Long> {

    /**
     * Find logs by user ID
     */
    List<CvOperationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find logs by user ID with pagination
     */
    Page<CvOperationLog> findByUserId(Long userId, Pageable pageable);

    /**
     * Find logs by action type
     */
    List<CvOperationLog> findByActionOrderByCreatedAtDesc(CvOperationAction action);

    /**
     * Find logs by action type with pagination
     */
    Page<CvOperationLog> findByAction(CvOperationAction action, Pageable pageable);

    /**
     * Find logs by source (INTERNAL or EXTERNAL_API)
     */
    Page<CvOperationLog> findBySource(String source, Pageable pageable);

    /**
     * Find logs within date range
     */
    @Query("SELECT c FROM CvOperationLog c WHERE c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    Page<CvOperationLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Count operations by action type
     */
    long countByAction(CvOperationAction action);

    /**
     * Count operations by source
     */
    long countBySource(String source);

    /**
     * Count successful operations
     */
    long countBySuccess(Boolean success);

    /**
     * Find all logs with pagination, ordered by created at desc
     */
    Page<CvOperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
