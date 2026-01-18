package com.app.civify.repository;

import com.app.civify.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    /**
     * Find all active push subscriptions for a user
     */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.user.id = :userId AND ps.active = true")
    List<PushSubscription> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Find subscription by endpoint (to avoid duplicates)
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);

    /**
     * Delete subscription by endpoint
     */
    void deleteByEndpoint(String endpoint);

    /**
     * Find all subscriptions for a user
     */
    List<PushSubscription> findByUserId(Long userId);
}
