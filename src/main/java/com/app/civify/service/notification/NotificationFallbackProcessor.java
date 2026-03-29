package com.app.civify.service.notification;

import com.app.civify.entity.Notification;
import com.app.civify.repository.NotificationRepository;
import com.app.civify.service.notification.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fallback processor for notifications.
 * 
 * PURPOSE: Catch notifications that weren't delivered via Pub/Sub
 * (e.g., if Pub/Sub was temporarily down, or SSE client wasn't connected)
 * 
 * PREVENTS DUPLICATES by checking delivered = FALSE flag.
 * PubSubNotificationListener marks as delivered = TRUE after sending.
 * 
 * Runs every 60 seconds to check for undelivered notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationFallbackProcessor {

    private final NotificationRepository notificationRepository;
    private final SseNotificationDeliveryService sseDeliveryService;

    /**
     * Process undelivered notifications every 60 seconds.
     * This catches notifications that Pub/Sub missed.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void processMissedNotifications() {
        try {
            List<Notification> undelivered = notificationRepository.findAllUndelivered(LocalDateTime.now());

            if (!undelivered.isEmpty()) {
                log.info("🔄 Fallback processor: Found {} undelivered notifications", undelivered.size());

                for (Notification notif : undelivered) {
                    // Build message for SSE delivery
                    NotificationMessage message = NotificationMessage.builder()
                        .id(notif.getId())
                        .userId(notif.getUser() != null ? notif.getUser().getId() : null)
                        .title(notif.getTitle())
                        .message(notif.getMessage())
                        .type(notif.getType().name())
                        .createdAt(notif.getCreatedAt().toString())
                        .metadata(notif.getMetadata())
                        .build();

                    // Attempt SSE delivery
                    boolean delivered = sseDeliveryService.deliver(message);

                    if (delivered) {
                        // Mark as delivered immediately so it won't be retried
                        notif.markAsDelivered();
                        log.debug("✅ Fallback delivered notification {} via SSE", notif.getId());
                    } else {
                        notif.incrementDeliveryAttempts();

                        // Mark as delivered if too many attempts (prevent infinite retries)
                        if (notif.getDeliveryAttempts() >= 5) {
                            log.warn("⚠️ Notification {} exceeded max delivery attempts, marking as delivered", notif.getId());
                            notif.markAsDelivered();
                        }
                    }

                    notificationRepository.save(notif);
                }
            }

        } catch (Exception e) {
            log.error("❌ Fallback processor error", e);
        }
    }

    /**
     * Cleanup old delivered notifications (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @org.springframework.transaction.annotation.Transactional
    public void cleanupOldNotifications() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            notificationRepository.deleteByDeliveredTrueAndDeliveredAtBefore(cutoff);
            log.info("🧹 Cleaned up notifications older than 30 days");
        } catch (Exception e) {
            log.error("Failed to cleanup old notifications", e);
        }
    }
}
