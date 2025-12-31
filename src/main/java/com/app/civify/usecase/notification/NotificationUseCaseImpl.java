package com.app.civify.usecase.notification;

import com.app.civify.entity.Notification;
import com.app.civify.entity.User;
import com.app.civify.repository.NotificationRepository;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.notification.PostgresNotificationListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationUseCaseImpl implements NotificationUseCase {

    private final PostgresNotificationListener listener;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getRecentNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findRecentNotificationsForUser(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUndeliveredForUser(userId);
    }

    @Override
    public SseEmitter subscribe(UserPrincipal userPrincipal) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout

        // Safely extract user from principal
        User user = userPrincipal != null ? userPrincipal.getUser() : null;
        if (user == null) {
            log.warn("SSE connection attempted without valid user principal");
            emitter.completeWithError(new IllegalStateException("User not authenticated"));
            return emitter;
        }

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
}
