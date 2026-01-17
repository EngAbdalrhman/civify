package com.app.civify.service.notification;

import com.app.civify.entity.Notification;
import com.app.civify.entity.User;
import com.app.civify.entity.enums.NotificationType;
import com.app.civify.repository.NotificationRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.service.notification.dto.NotificationMessage;
import com.app.civify.service.notification.pubsub.PubSubNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Notification service using Google Pub/Sub as message queue.
 * 
 * HOW IT WORKS:
 * 
 * 1. Admin sends broadcast:
 *    notificationService.sendToAll("Maintenance", "System down at 10 PM", "INFO")
 *    → Saves to DB with user_id = NULL
 *    → Publishes to Pub/Sub topic
 *    → PubSubNotificationListener receives → pushes to ALL connected SSE clients
 * 
 * 2. System sends to user:
 *    notificationService.sendToUser(userId, "Payment Success", "Premium activated", "SUCCESS")
 *    → Saves to DB with specific user_id
 *    → Publishes to Pub/Sub topic
 *    → PubSubNotificationListener receives → pushes to THAT user's SSE connection + Web Push
 * 
 * 3. DEDUPLICATION:
 *    Use idempotencyKey to prevent duplicates
 *    notificationService.sendToUser(userId, title, message, "SUCCESS", "payment_123_success")
 *    → Checks if idempotencyKey exists → skip if already sent
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PubSubNotificationPublisher pubSubPublisher;

    /**
     * Send notification to ALL users (broadcast).
     * Saved with user_id = NULL in database.
     */
    @Override
    @Async
    @Transactional
    public void sendToAll(String title, String message, NotificationType type) {
        log.info("📢 Broadcasting notification to ALL users: [{}] {}", type, title);

        try {
            Notification notification = Notification.builder()
                    .user(null) // NULL = broadcast
                    .title(title)
                    .message(message)
                    .type(type)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ Broadcast notification saved: id={}", notification.getId());

            // Publish to Pub/Sub for real-time delivery
            publishToPubSub(notification);

        } catch (Exception e) {
            log.error("❌ Failed to create broadcast notification", e);
        }
    }

    /**
     * Send notification to specific user.
     * Saved with specific user_id in database.
     */
    @Override
    @Async
    @Transactional
    public void sendToUser(Long userId, String title, String message, NotificationType type) {
        sendToUser(userId, title, message, type, null, null);
    }

    /**
     * Send notification with metadata and idempotency key.
     * 
     * @param idempotencyKey Unique key to prevent duplicates (e.g., "payment_order_123_success")
     */
    @Override
    @Transactional
    public void sendToUser(Long userId, String title, String message, NotificationType type,
                          String idempotencyKey, Map<String, Object> metadata) {
        log.info("📬 Sending notification to user {}: [{}] {}", userId, type, title);

        try {
            // Check for duplicate (idempotency)
            if (idempotencyKey != null) {
                Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    log.warn("⚠️ Duplicate notification detected (idempotencyKey={}), skipping", idempotencyKey);
                    return;
                }
            }

            // Fetch user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Create notification
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(type)
                    .idempotencyKey(idempotencyKey)
                    .metadata(metadata)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ User notification saved: id={}, userId={}", notification.getId(), userId);

            // Publish to Pub/Sub for real-time delivery
            publishToPubSub(notification);

        } catch (Exception e) {
            log.error("❌ Failed to create notification for user {}", userId, e);
        }
    }

    /**
     * Publish notification to Pub/Sub for real-time delivery
     */
    private void publishToPubSub(Notification notification) {
        NotificationMessage message = NotificationMessage.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .createdAt(notification.getCreatedAt().toString())
                .metadata(notification.getMetadata())
                .build();

        pubSubPublisher.publish(message);
    }

    /**
     * Schedule notification for future delivery
     */
    @Transactional
    public void scheduleNotification(Long userId, String title, String message, NotificationType type,
                                    LocalDateTime scheduledFor) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .scheduledFor(scheduledFor)
                .build();

        notificationRepository.save(notification);
        log.info("📅 Scheduled notification for {}: id={}", scheduledFor, notification.getId());
    }
}
