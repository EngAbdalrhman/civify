package com.app.civify.service.notification;

public interface NotificationService {
    void sendToAll(String title, String message, com.app.civify.entity.enums.NotificationType type);
    void sendToUser(Long userId, String title, String message, com.app.civify.entity.enums.NotificationType type);
    void sendToUser(Long userId, String title, String message, com.app.civify.entity.enums.NotificationType type, String idempotencyKey, java.util.Map<String, Object> metadata);
}
