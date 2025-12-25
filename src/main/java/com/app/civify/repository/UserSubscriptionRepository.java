package com.app.civify.repository;

import com.app.civify.entity.plans.UserSubscription;
import com.app.civify.entity.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // Admin queries
    Page<UserSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);
}
