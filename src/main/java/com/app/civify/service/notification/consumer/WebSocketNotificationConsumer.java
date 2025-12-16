package com.app.civify.service.notification.consumer;

import com.app.civify.dto.notification.NotificationMessage;
import com.app.civify.entity.Notification;
import com.app.civify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumer for WebSocket notification delivery.
 * Listens to RabbitMQ websocket queue and pushes to connected WebSocket clients.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketNotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    /**
     * Listen to WebSocket queue and deliver notifications via WebSocket.
     * 
     * Architecture:
     * - Broadcast notifications -> /topic/notifications (all connected users)
     * - User-specific notifications -> /queue/notifications/{userId} (specific user)
     */
    @RabbitListener(queues = "notification.websocket")
    public void handleWebSocketNotification(NotificationMessage message) {
        try {
            log.info("📱 WebSocket consumer received: {} for {}", 
                message.getTitle(), 
                message.isBroadcast() ? "ALL USERS" : "user " + message.getUserId());

            if (message.isBroadcast()) {
                // Send to all connected WebSocket clients
                messagingTemplate.convertAndSend("/topic/notifications", message);
                log.info("✅ Broadcast notification sent to /topic/notifications");
            } else {
                // Send to specific user
                messagingTemplate.convertAndSendToUser(
                        message.getUserId().toString(),
                        "/queue/notifications",
                        message
                );
                log.info("✅ User notification sent to WebSocket for user {}", message.getUserId());

                // Mark as delivered in database
                updateDeliveryStatus(message.getNotificationId(), "websocket");
            }

        } catch (Exception e) {
            log.error("❌ Failed to deliver WebSocket notification: {}", message.getTitle(), e);
            // Message will be sent to DLQ after retries
            throw e; // Re-throw to trigger RabbitMQ retry/DLQ
        }
    }

    /**
     * Update delivery status in database
     */
    private void updateDeliveryStatus(Long notificationId, String channel) {
        if (notificationId != null) {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                if ("websocket".equals(channel)) {
                    notification.setWebsocketDelivered(true);
                }
                notificationRepository.save(notification);
            });
        }
    }
}
