package com.app.civify.service.notification;

import com.app.civify.config.RabbitMQConfig;
import com.app.civify.dto.notification.NotificationMessage;
import com.app.civify.entity.Notification;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.NotificationType;
import com.app.civify.repository.NotificationRepository;
import com.app.civify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Send notification to all users (broadcast).
     * Published to fanout exchange which routes to all channels.
     */
    @Override
    @Async
    @Transactional
    public void sendToAll(String title, String message, String type) {
        log.info("📢 Broadcasting notification to ALL users: [{}] {}", type, title);

        try {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());

            // 1. Save broadcast notification to database
            Notification notification = Notification.builder()
                    .user(null) // null = broadcast
                    .title(title)
                    .message(message)
                    .type(notificationType)
                    .channels("WEBSOCKET,EMAIL")
                    .build();
            notification = notificationRepository.save(notification);

            // 2. Create message for RabbitMQ
            NotificationMessage rabbitMessage = NotificationMessage.builder()
                    .notificationId(notification.getId())
                    .userId(null)
                    .title(title)
                    .message(message)
                    .type(notificationType)
                    .channels(Set.of("WEBSOCKET", "EMAIL"))
                    .broadcast(true)
                    .build();

            // 3. Publish to fanout exchange (broadcasts to all queues)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_FANOUT_EXCHANGE,
                    "", // Fanout doesn't need routing key
                    rabbitMessage
            );

            log.info("✅ Broadcast notification published to RabbitMQ: notification_id={}", notification.getId());

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid notification type: {}", type);
        } catch (Exception e) {
            log.error("❌ Failed to send broadcast notification", e);
        }
    }

    /**
     * Send notification to a specific user.
     * Published to direct exchange with routing to specific channels.
     */
    @Override
    @Async
    @Transactional
    public void sendToUser(Long userId, String title, String message, String type) {
        sendToUser(userId, title, message, type, Set.of("WEBSOCKET", "EMAIL"));
    }

    /**
     * Send notification to a specific user with custom channels.
     */
    @Transactional
    public void sendToUser(Long userId, String title, String message, String type, Set<String> channels) {
        log.info("📬 Sending notification to user {}: [{}] {}", userId, type, title);

        try {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());

            // 1. Fetch user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // 2. Save notification to database
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(notificationType)
                    .channels(String.join(",", channels))
                    .build();
            notification = notificationRepository.save(notification);

            // 3. Create message for RabbitMQ
            NotificationMessage rabbitMessage = NotificationMessage.builder()
                    .notificationId(notification.getId())
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(notificationType)
                    .channels(channels)
                    .broadcast(false)
                    .build();

            // 4. Publish to each requested channel
            for (String channel : channels) {
                String routingKey = getRoutingKeyForChannel(channel);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_DIRECT_EXCHANGE,
                        routingKey,
                        rabbitMessage
                );
                log.debug("✉️ Published to channel: {} for user {}", channel, userId);
            }

            log.info("✅ User notification published to RabbitMQ: notification_id={}, channels={}", 
                notification.getId(), channels);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid notification type: {}", type);
        } catch (Exception e) {
            log.error("❌ Failed to send notification to user {}", userId, e);
        }
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser() != null && notification.getUser().getId().equals(userId)) {
                notification.markAsRead();
                notificationRepository.save(notification);
                log.info("✅ Notification {} marked as read by user {}", notificationId, userId);
            }
        });
    }

    /**
     * Get routing key for a specific channel
     */
    private String getRoutingKeyForChannel(String channel) {
        return switch (channel.toUpperCase()) {
            case "WEBSOCKET" -> RabbitMQConfig.WEBSOCKET_ROUTING_KEY;
            case "EMAIL" -> RabbitMQConfig.EMAIL_ROUTING_KEY;
            case "FCM" -> RabbitMQConfig.FCM_ROUTING_KEY;
            default -> RabbitMQConfig.WEBSOCKET_ROUTING_KEY;
        };
    }
}
