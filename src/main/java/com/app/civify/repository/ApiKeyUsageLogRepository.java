package com.app.civify.repository;

import com.app.civify.entity.ApiKeyUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyUsageLogRepository extends JpaRepository<ApiKeyUsageLog, Long> {
    
    /**
     * Count API usage logs for a user in a specific month.
     * Uses PostgreSQL TO_CHAR function for date formatting.
     */
    @Query("SELECT COUNT(l) FROM ApiKeyUsageLog l WHERE l.user.id = :userId AND FUNCTION('TO_CHAR', l.timestamp, 'YYYY-MM') = :month")
    long countByUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);
}

