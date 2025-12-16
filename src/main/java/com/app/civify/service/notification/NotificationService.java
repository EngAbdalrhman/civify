package com.app.civify.service.notification;

public interface NotificationService {
    void sendToAll(String title, String message, String type);
    void sendToUser(Long userId, String title, String message, String type);
}
