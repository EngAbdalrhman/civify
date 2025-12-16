package com.app.civify.repository;

import com.app.civify.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find undelivered notifications for a specific user
     */
    List<Notification> findByUserIdAndDeliveredFalseOrderByCreatedAtAsc(Long userId);

    /**
     * Find undelivered broadcast notifications (user_id IS NULL)
     */
    @Query("SELECT n FROM Notification n WHERE n.user IS NULL AND n.delivered = false ORDER BY n.createdAt ASC")
    List<Notification> findUndeliveredBroadcasts();

    /**
     * Find all undelivered notifications (for Quartz fallback)
     */
    @Query("SELECT n FROM Notification n WHERE n.delivered = false AND (n.scheduledFor IS NULL OR n.scheduledFor <= :now) ORDER BY n.createdAt ASC")
    List<Notification> findAllUndelivered(@Param("now") LocalDateTime now);

    /**
     * Find notification by idempotency key (prevent duplicates)
     */
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    /**
     * Delete old delivered notifications (cleanup job)
     */
    void deleteByDeliveredTrueAndDeliveredAtBefore(LocalDateTime before);

    /**
     * Count undelivered notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.user.id = :userId OR n.user IS NULL) AND n.delivered = false")
    Long countUndeliveredForUser(@Param("userId") Long userId);
}
