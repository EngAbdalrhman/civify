package com.app.civify.service.notification.consumer;

import com.app.civify.dto.notification.NotificationMessage;
import com.app.civify.entity.Notification;
import com.app.civify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for FCM (Firebase Cloud Messaging) push notification delivery.
 * Listens to RabbitMQ FCM queue and sends push notifications to mobile devices.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FcmNotificationConsumer {

    private final NotificationRepository notificationRepository;
    // TODO: Add FCM service when Firebase is configured
    // private final FcmService fcmService;

    /**
     * Listen to FCM queue and send push notifications.
     */
    @RabbitListener(queues = "notification.fcm")
    public void handleFcmNotification(NotificationMessage message) {
        try {
            log.info("🔔 FCM consumer received: {} for {}", 
                message.getTitle(), 
                message.isBroadcast() ? "ALL USERS" : "user " + message.getUserId());

            // TODO: Implement FCM sending when Firebase is configured
            /*
            if (message.isBroadcast()) {
                fcmService.sendToAllDevices(message);
            } else if (message.getUserId() != null) {
                fcmService.sendToUser(message.getUserId(), message);
                updateDeliveryStatus(message.getNotificationId(), "fcm");
            }
            */

            log.info("📱 FCM delivery placeholder - Firebase not yet configured");
            updateDeliveryStatus(message.getNotificationId(), "fcm");

        } catch (Exception e) {
            log.error("❌ Failed to send FCM notification: {}", message.getTitle(), e);
            throw e; // Re-throw to trigger RabbitMQ retry/DLQ
        }
    }

    /**
     * Update delivery status in database
     */
    private void updateDeliveryStatus(Long notificationId, String channel) {
        if (notificationId != null) {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                if ("fcm".equals(channel)) {
                    notification.setFcmSent(true);
                }
                notificationRepository.save(notification);
            });
        }
    }
}
