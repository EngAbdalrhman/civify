package com.app.civify.dto.notification;

import com.app.civify.entity.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for notification messages sent through RabbitMQ.
 * Must be Serializable for RabbitMQ message conversion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Notification ID from database (if persisted)
     */
    private Long notificationId;

    /**
     * Target user ID (null = broadcast)
     */
    private Long userId;

    private String title;

    private String message;

    private NotificationType type;

    /**
     * Delivery channels: WEBSOCKET, EMAIL, FCM
     */
    @Builder.Default
    private Set<String> channels = Set.of("WEBSOCKET");

    /**
     * Whether this is a broadcast message
     */
    @Builder.Default
    private boolean broadcast = false;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Additional metadata (e.g., action URL, image URL)
     */
    private String metadata;
}
