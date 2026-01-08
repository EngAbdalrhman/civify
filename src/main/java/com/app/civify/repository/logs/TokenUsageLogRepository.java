package com.app.civify.repository.logs;

import com.app.civify.entity.enums.TokenUsageType;
import com.app.civify.entity.logs.TokenUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, Long>,
                                                  JpaSpecificationExecutor<TokenUsageLog> {

    /**
     * Find usage logs by user with pagination (for user's own history)
     */
    Page<TokenUsageLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find usage logs by user and type
     */
    List<TokenUsageLog> findByUserIdAndUsageTypeOrderByCreatedAtDesc(Long userId, TokenUsageType usageType);

    /**
     * Sum total tokens used by a user
     */
    @Query("SELECT COALESCE(SUM(t.tokensUsed), 0) FROM TokenUsageLog t WHERE t.userId = :userId")
    Integer sumTokensByUserId(@Param("userId") Long userId);

    /**
     * Sum tokens used by user within a time period (for monthly summary)
     */
    @Query("SELECT COALESCE(SUM(t.tokensUsed), 0) FROM TokenUsageLog t WHERE t.userId = :userId " +
           "AND t.createdAt >= :start AND t.createdAt <= :end")
    Integer sumTokensByUserIdAndPeriod(@Param("userId") Long userId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * Sum tokens used from subscription by user within a time period
     */
    @Query("SELECT COALESCE(SUM(t.fromSubscription), 0) FROM TokenUsageLog t WHERE t.userId = :userId " +
           "AND t.createdAt >= :start AND t.createdAt <= :end")
    Integer sumFromSubscriptionByUserIdAndPeriod(@Param("userId") Long userId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /**
     * Sum tokens used from wallet by user within a time period
     */
    @Query("SELECT COALESCE(SUM(t.fromWallet), 0) FROM TokenUsageLog t WHERE t.userId = :userId " +
           "AND t.createdAt >= :start AND t.createdAt <= :end")
    Integer sumFromWalletByUserIdAndPeriod(@Param("userId") Long userId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
