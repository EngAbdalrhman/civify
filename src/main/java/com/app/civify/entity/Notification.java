package com.app.civify.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.app.civify.entity.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification entity for PostgreSQL-based message queue.
 *
 * How it works: 1. Admin calls notificationService.sendToAll() → saved with
 * user_id = NULL (broadcast) 2. System calls notificationService.sendToUser() →
 * saved with specific user_id 3. PostgreSQL TRIGGER fires → sends pg_notify()
 * 4. Backend listener receives → pushes via SSE to frontend 5. Mark as
 * delivered = true to prevent re-sending
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Target user ID. NULL = broadcast to all users NOT NULL = send to specific
     * user only
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * Delivery tracking to prevent duplicates. When notification is pushed to
     * frontend via SSE, set to TRUE.
     */
    @Builder.Default
    @Column(name = "delivered")
    private Boolean delivered = false;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Builder.Default
    @Column(name = "delivery_attempts")
    private Integer deliveryAttempts = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    /**
     * Extra metadata (action URL, image URL, etc.)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Idempotency key to prevent duplicate notifications. Example:
     * "payment_order_12345_success"
     */
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    /**
     * Read tracking — TRUE when user has acknowledged/read the notification.
     */
    @Builder.Default
    @Column(name = "read")
    private Boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Check if this is a broadcast notification
     */
    public boolean isBroadcast() {
        return user == null;
    }

    /**
     * Mark notification as successfully delivered
     */
    public void markAsDelivered() {
        this.delivered = true;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * Mark notification as read by the user
     */
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Increment delivery attempt counter
     */
    public void incrementDeliveryAttempts() {
        this.deliveryAttempts++;
        this.lastAttemptAt = LocalDateTime.now();
    }
}
