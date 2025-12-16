package com.app.civify.repository;

import com.app.civify.entity.Notification;
import com.app.civify.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a specific user
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find unread notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAtDESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Find all broadcast notifications (user_id is null)
     */
    @Query("SELECT n FROM Notification n WHERE n.user IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findAllBroadcastNotifications();

    /**
     * Find notifications by type for a user
     */
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);

    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Delete old notifications (older than specified date)
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
}
