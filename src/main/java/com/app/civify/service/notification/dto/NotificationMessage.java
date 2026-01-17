package com.app.civify.service.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for notification message payload used in Pub/Sub messaging.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
    private Long id;
    private Long userId;  // null = broadcast to all users
    private String title;
    private String message;
    private String type;
    private String createdAt;
    private Map<String, Object> metadata;
}
