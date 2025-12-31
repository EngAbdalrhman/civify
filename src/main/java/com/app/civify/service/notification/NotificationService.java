package com.app.civify.service.notification;

import com.app.civify.entity.enums.NotificationType;

import java.util.Map;

public interface NotificationService {
    void sendToAll(String title, String message, NotificationType type);

    void sendToUser(Long userId, String title, String message, NotificationType type);

    void sendToUser(Long userId, String title, String message, NotificationType type, String idempotencyKey, Map<String, Object> metadata);
}
