package com.app.civify.repository;

import com.app.civify.entity.ApiUsageLog;
import com.app.civify.util.persistence.CommonRepo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiUsageLogRepository extends CommonRepo<ApiUsageLog, Long> {

    List<ApiUsageLog> findByUserId(Long userId);

    List<ApiUsageLog> findByUserIdAndMonth(Long userId, String month);

    List<ApiUsageLog> findByUserIdAndTimestampBetween(Long userId, LocalDateTime start, LocalDateTime end);

    Long countByUserIdAndMonth(Long userId, String month);
}
