package com.app.civify.repository;

import com.app.civify.entity.User;
import com.app.civify.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserAndStatus(User user, UserSubscription.SubscriptionStatus status);

    Optional<UserSubscription> findByUserAndStatusAndEndDateAfter(User user, UserSubscription.SubscriptionStatus status, LocalDateTime date);

    Optional<UserSubscription> findByPaymentTransactionId(String paymentTransactionId);

    // For lifecycle management
    List<UserSubscription> findByStatusAndEndDateBefore(UserSubscription.SubscriptionStatus status, LocalDateTime date);
    List<UserSubscription> findByStatusAndEndDateBetween(UserSubscription.SubscriptionStatus status, LocalDateTime startDate, LocalDateTime endDate);
}
