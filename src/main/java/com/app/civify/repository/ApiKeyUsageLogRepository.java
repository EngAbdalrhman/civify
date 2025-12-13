package com.app.civify.repository;

import com.app.civify.entity.ApiKeyUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyUsageLogRepository extends JpaRepository<ApiKeyUsageLog, Long> {
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(l) FROM ApiKeyUsageLog l WHERE l.user.id = :userId AND FUNCTION('DATE_FORMAT', l.timestamp, '%Y-%m') = :month")
    long countByUserIdAndMonth(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("month") String month);
}
