package com.app.civify.entity;

import com.app.civify.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification entity for persisting notification history.
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
     * Target user (null = broadcast to all users)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * Channels to deliver notification through
     * Comma-separated: WEBSOCKET,EMAIL,FCM
     */
    @Builder.Default
    @Column(name = "channels")
    private String channels = "WEBSOCKET";

    /**
     * Whether notification has been read by user
     */
    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    /**
     * Whether email was sent successfully
     */
    @Builder.Default
    @Column(name = "email_sent")
    private Boolean emailSent = false;

    /**
     * Whether FCM push was sent successfully
     */
    @Builder.Default
    @Column(name = "fcm_sent")
    private Boolean fcmSent = false;

    /**
     * Whether WebSocket delivery was acknowledged
     */
    @Builder.Default
    @Column(name = "websocket_delivered")
    private Boolean websocketDelivered = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Check if this is a broadcast notification
     */
    public boolean isBroadcast() {
        return user == null;
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
