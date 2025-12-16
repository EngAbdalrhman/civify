package com.app.civify.controller.v1;

import com.app.civify.entity.Notification;
import com.app.civify.entity.User;
import com.app.civify.repository.NotificationRepository;
import com.app.civify.service.notification.PostgresNotificationListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SSE (Server-Sent Events) endpoint for real-time notifications.
 * 
 * FRONTEND USAGE:
 * ```javascript
 * const eventSource = new EventSource('/api/v1/notifications/stream', {
 *   withCredentials: true
 * });
 * 
 * eventSource.addEventListener('notification', (event) => {
 *   const notification = JSON.parse(event.data);
 *   showToast(notification.title, notification.message);
 * });
 * ```
 * 
 * DEDUPLICATION:
 * - Each notification sent ONCE via SSE
 * - Marked as delivered = TRUE immediately after sending
 * - Quartz fallback skips delivered = TRUE notifications
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final PostgresNotificationListener listener;
    private final NotificationRepository notificationRepository;

    /**
     * SSE stream endpoint for real-time notifications.
     * Auto-sends any missed notifications on connection.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout

        try {
            // Register connection with listener
            listener.registerUserConnection(user.getId(), emitter);
            listener.registerBroadcastConnection(emitter); // Also receive broadcasts

            // Send any undelivered notifications immediately
            sendMissedNotifications(user.getId(), emitter);

            log.info("✅ SSE connection established for user {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to establish SSE connection for user {}", user.getId(), e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Send any notifications that were created while user was offline
     */
    private void sendMissedNotifications(Long userId, SseEmitter emitter) {
        try {
            // Get undelivered user-specific notifications
            List<Notification> missed = notificationRepository.findByUserIdAndDeliveredFalseOrderByCreatedAtAsc(userId);
            
            // Get undelivered broadcast notifications
            List<Notification> broadcasts = notificationRepository.findUndeliveredBroadcasts();

            int totalMissed = missed.size() + broadcasts.size();
            
            if (totalMissed > 0) {
                log.info("📬 Sending {} missed notifications to user {}", totalMissed, userId);

                // Send user-specific notifications
                for (Notification notif : missed) {
                    sendNotification(emitter, notif);
                }

                // Send broadcast notifications
                for (Notification notif : broadcasts) {
                    sendNotification(emitter, notif);
                }
            }

        } catch (Exception e) {
            log.error("Failed to send missed notifications to user {}", userId, e);
        }
    }

    /**
     * Send single notification via SSE
     */
    private void sendNotification(SseEmitter emitter, Notification notif) {
        try {
            Map<String, Object> data = Map.of(
                "id", notif.getId(),
                "title", notif.getTitle(),
                "message", notif.getMessage(),
                "type", notif.getType(),
                "timestamp", notif.getCreatedAt().toString()
            );

            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(data));

            // Mark as delivered (PREVENTS DUPLICATE SENDS)
            notif.markAsDelivered();
            notificationRepository.save(notif);

        } catch (IOException e) {
            log.warn("Failed to send notification {} via SSE", notif.getId());
        }
    }

    /**
     * Get undelivered notification count for user
     */
    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal User user) {
        Long count = notificationRepository.countUndeliveredForUser(user.getId());
        return Map.of("count", count);
    }
}
