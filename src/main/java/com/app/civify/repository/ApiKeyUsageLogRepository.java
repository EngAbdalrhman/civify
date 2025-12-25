package com.app.civify.repository;

import com.app.civify.entity.logs.ApiKeyUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ApiKeyUsageLogRepository extends JpaRepository<ApiKeyUsageLog, Long>, JpaSpecificationExecutor<ApiKeyUsageLog> {
    
    /**
     * Count API usage logs for a user in a specific month.
     * Uses PostgreSQL TO_CHAR function for date formatting.
     */
    @Query("SELECT COUNT(l) FROM ApiKeyUsageLog l WHERE l.user.id = :userId AND FUNCTION('TO_CHAR', l.timestamp, 'YYYY-MM') = :month")
    long countByUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);
    
    /**
     * Find all API usage logs for a specific user with pagination.
     */
    Page<ApiKeyUsageLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    /**
     * Find API usage logs for a user within a date range.
     */
    @Query("SELECT l FROM ApiKeyUsageLog l WHERE l.user.id = :userId AND l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    Page<ApiKeyUsageLog> findByUserIdAndDateRange(
        @Param("userId") Long userId, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        Pageable pageable
    );
    
    /**
     * Find API usage logs for a user filtered by endpoint.
     */
    Page<ApiKeyUsageLog> findByUserIdAndEndpointContainingIgnoreCaseOrderByTimestampDesc(
        Long userId, String endpoint, Pageable pageable
    );
    
    /**
     * Count success/failure for user stats.
     */
    long countByUserIdAndSuccess(Long userId, boolean success);
}
