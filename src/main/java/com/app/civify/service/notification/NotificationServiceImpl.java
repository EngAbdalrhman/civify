package com.app.civify.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendToAll(String title, String message, String type) {
        log.info("Sending notification to ALL users: [{}] {} - {}", type, title, message);
        // Implement actual notification logic here (e.g. iterate users, push to websocket/FCM)
    }

    @Override
    public void sendToUser(Long userId, String title, String message, String type) {
        log.info("Sending notification to user {}: [{}] {} - {}", userId, type, title, message);
    }
}
