package com.app.civify.repository;

import com.app.civify.entity.UserSubscription;
import com.app.civify.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByPaymentTransactionId(String paymentTransactionId);

    // For lifecycle management
    List<UserSubscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime date);

    List<UserSubscription> findByStatusAndEndDateBetween(SubscriptionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    List<UserSubscription> findByStatusAndStartDateBefore(SubscriptionStatus status, LocalDateTime date);

    List<UserSubscription> findByStatusAndStartDateBetween(SubscriptionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // Subscription queries by user ID
    List<UserSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<UserSubscription> findByUserId(Long userId);

    @Query("SELECT s FROM UserSubscription s WHERE s.user.id = :userId AND s.plan.name = :planName")
    Optional<UserSubscription> findByUserIdAndPlanName(@Param("userId") Long userId, @Param("planName") String planName);
}
