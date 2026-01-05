package com.app.civify.repository.plans;

import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long>, JpaSpecificationExecutor<UserSubscription> {

    Optional<UserSubscription> findByPaymentTransactionId(String paymentTransactionId);

    // For lifecycle management
    List<UserSubscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime date);

    List<UserSubscription> findByStatusAndEndDateBetween(SubscriptionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    List<UserSubscription> findByStatusAndStartDateBefore(SubscriptionStatus status, LocalDateTime date);

    List<UserSubscription> findByStatusAndStartDateBetween(SubscriptionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // Subscription queries by user ID
    List<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    Optional<UserSubscription> findByUserIdAndPlanIdAndStatus(Long userId, Long planId, SubscriptionStatus status);

    List<UserSubscription> findByUserId(Long userId);

    @Query("SELECT s FROM UserSubscription s WHERE s.user.id = :userId AND s.plan.name = :planName")
    Optional<UserSubscription> findByUserIdAndPlanName(@Param("userId") Long userId, @Param("planName") String planName);

    @Query("SELECT COUNT(s) FROM UserSubscription s WHERE s.plan.id = :planId AND s.status = 'ACTIVE'")
    long countActiveSubscriptionsByPlanId(@Param("planId") Long planId);

    // Count subscriptions by user
    Long countByUserId(Long userId);

    Long countByUserIdAndStatus(Long userId, SubscriptionStatus status);

    @Query("SELECT COUNT(s) FROM UserSubscription s WHERE s.status = :status AND LOWER(s.plan.name) != LOWER(:excludePlanName)")
    long countByStatusAndPlanNameNotIgnoreCase(@Param("status") SubscriptionStatus status, @Param("excludePlanName") String excludePlanName);

    // Admin queries
    Page<UserSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    // For billing cycle - find subscriptions where usageResetDate has passed  
    List<UserSubscription> findByUsageResetDateBefore(LocalDateTime date);
    
    // For invoicing - find renewable subscriptions expiring in specified window

    @Query("SELECT s FROM UserSubscription s WHERE s.status = :status " +
           "AND s.plan.renewalAllowed = true " +
           "AND s.endDate BETWEEN :startDate AND :endDate " +
           "AND LOWER(s.plan.name) != 'free'")
    List<UserSubscription> findRenewableSubscriptionsExpiringBetween(
            @Param("status") SubscriptionStatus status, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    // ==================================================================================
    // OPTIMIZED TOKEN OPERATIONS
    // ==================================================================================

    /**
     * Projection for efficient token balance checks.
     * Avoids loading the full UserSubscription and related entities.
     */
    interface TokenInfo {
        Long getId();
        Integer getCurrentTokenUsage();
        Integer getTokenLimit(); // From plan
        LocalDateTime getUsageResetDate();
        SubscriptionStatus getStatus();
        LocalDateTime getEndDate();
    }

    /**
     * Get token info without loading full subscription + plan entities.
     * Fetches plan token limit eagerly in the projection.
     */
    @Query("SELECT s.id as id, s.currentTokenUsage as currentTokenUsage, " +
           "p.tokenLimit as tokenLimit, s.usageResetDate as usageResetDate, " +
           "s.status as status, s.endDate as endDate " +
           "FROM UserSubscription s JOIN s.plan p " +
           "WHERE s.user.id = :userId AND s.status = :status")
    Optional<TokenInfo> findTokenInfoByUserIdAndStatus(@Param("userId") Long userId, 
                                                     @Param("status") SubscriptionStatus status);

    /**
     * Update token usage directly with a single UPDATE query.
     */
    @Modifying
    @Query("UPDATE UserSubscription s SET s.currentTokenUsage = :usage WHERE s.id = :id")
    int updateTokenUsage(@Param("id") Long id, @Param("usage") Integer usage);
}
