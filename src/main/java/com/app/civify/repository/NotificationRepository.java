package com.app.civify.repository;

import com.app.civify.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = "SELECT * FROM notifications WHERE (user_id = :userId OR user_id IS NULL) ORDER BY created_at DESC",
           nativeQuery = true)
    Page<Notification> findRecentNotificationsForUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find undelivered notifications for a specific user
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.delivered = false ORDER BY n.createdAt ASC")
    List<Notification> findByUserIdAndDeliveredFalseOrderByCreatedAtAsc(@Param("userId") Long userId);

    /**
     * Find undelivered broadcast notifications (user_id IS NULL)
     */
    @Query("SELECT n FROM Notification n WHERE n.user IS NULL AND n.delivered = false")
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

    /**
     * Count unread notifications for a user (user-specific + broadcasts)
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.user.id = :userId OR n.user IS NULL) AND n.read = false")
    Long countUnreadForUser(@Param("userId") Long userId);

    /**
     * Mark a single notification as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :notificationId AND (n.user.id = :userId OR n.user IS NULL)")
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    /**
     * Mark all unread notifications as read for a user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE (n.user.id = :userId OR n.user IS NULL) AND n.read = false")
    int markAllAsReadForUser(@Param("userId") Long userId);
}
