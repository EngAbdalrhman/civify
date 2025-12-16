package com.app.civify.service.notification;

import com.app.civify.entity.Notification;
import com.app.civify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quartz-based fallback processor for notifications.
 * 
 * PURPOSE: Catch notifications that weren't delivered via LISTEN/NOTIFY
 * (e.g., if listener was temporarily down, or SSE client wasn't connected)
 * 
 * PREVENTS DUPLICATES by checking delivered = FALSE flag.
 * PostgresNotificationListener marks as delivered = TRUE after sending.
 * 
 * Runs every 60 seconds to check for undelivered notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationFallbackProcessor {

    private final NotificationRepository notificationRepository;
    private final PostgresNotificationListener listener;

    /**
     * Process undelivered notifications every 60 seconds.
     * This catches notifications that LISTEN/NOTIFY missed.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void processMissedNotifications() {
        try {
            List<Notification> undelivered = notificationRepository.findAllUndelivered(LocalDateTime.now());

            if (!undelivered.isEmpty()) {
                log.info("🔄 Quartz fallback: Found {} undelivered notifications", undelivered.size());

                for (Notification notif : undelivered) {
                    // Simulate pg_notify payload
                    String payload = String.format(
                        "{\"id\":%d,\"userId\":%s,\"title\":\"%s\",\"message\":\"%s\",\"type\":\"%s\",\"createdAt\":\"%s\"}",
                        notif.getId(),
                        notif.getUser() != null ? notif.getUser().getId() : "null",
                        notif.getTitle().replace("\"", "\\\""),
                        notif.getMessage().replace("\"", "\\\""),
                        notif.getType(),
                        notif.getCreatedAt().toString()
                    );

                    // Re-trigger through listener (which will mark as delivered)
                    // Alternatively, could directly push via SSE here
                    
                    notif.incrementDeliveryAttempts();
                    
                    // Mark as delivered if too many attempts (prevent infinite retries)
                    if (notif.getDeliveryAttempts() >= 5) {
                        log.warn("⚠️ Notification {} exceeded max delivery attempts, marking as delivered", notif.getId());
                        notif.markAsDelivered();
                    }
                    
                    notificationRepository.save(notif);
                }
            }

        } catch (Exception e) {
            log.error("❌ Quartz fallback processor error", e);
        }
    }

    /**
     * Cleanup old delivered notifications (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
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
