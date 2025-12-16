package com.app.civify.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final FcmService fcmService;

    @Override
    @Async
    public void sendToAll(String title, String message, String type) {
        log.info("📢 Sending broadcast notification: [{}] {}", type, title);
        fcmService.sendToAll(title, message);
    }

    @Override
    @Async
    public void sendToUser(Long userId, String title, String message, String type) {
        log.info("📬 Sending notification to user {}: [{}] {}", userId, type, title);
        fcmService.sendToUser(userId, title, message);
    }
}
