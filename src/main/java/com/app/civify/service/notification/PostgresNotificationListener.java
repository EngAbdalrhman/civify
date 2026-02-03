package com.app.civify.service.notification;

import com.app.civify.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @deprecated This class is disabled in favor of Google Pub/Sub.
 * PostgreSQL LISTEN/NOTIFY listener for real-time notification delivery.
 * 
 * DISABLED: This implementation used PostgreSQL pg_notify() for real-time notifications.
 * Now using Google Cloud Pub/Sub via PubSubNotificationListener.
 * 
 * Kept for reference and potential fallback reactivation.
 * 
 * HOW TO PREVENT DUPLICATE SENDS:
 * 
 * 1. PostgreSQL TRIGGER fires ONCE per INSERT
 * 2. This listener receives notification
 * 3. Pushes to SSE clients
 * 4. Marks as delivered = TRUE in DB
 * 5. Quartz fallback skips delivered = TRUE notifications
 * 
 * DEDUPLICATION MECHANISMS:
 * - idempotencyKey in DB (prevents duplicate creation)
 * - delivered flag (prevents duplicate sending)
 * - Set<Long> processedIds cache (in-memory, prevents same notification twice)
 */

@Component
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class PostgresNotificationListener {

    private final DataSource dataSource;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    // SSE connections: userId -> list of SSE emitters
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();
    
    // Broadcast SSE connections (for broadcast notifications)
    private final CopyOnWriteArrayList<SseEmitter> broadcastConnections = new CopyOnWriteArrayList<>();

    private volatile boolean running = false; // DISABLED - set to false
    private Thread listenerThread;

    /**
     * DISABLED - PostgreSQL LISTEN/NOTIFY is no longer used.
     * Using Google Cloud Pub/Sub instead.
     */
    @PostConstruct
    public void startListening() {
        // DISABLED: PostgreSQL LISTEN/NOTIFY replaced by Google Pub/Sub
        log.info("⚠️ PostgreSQL LISTEN/NOTIFY is DISABLED. Using Google Pub/Sub for notifications.");
        
        // Original code kept for reference:
        // listenerThread = new Thread(this::listen, "PostgreSQL-LISTEN-Thread");
        // listenerThread.setDaemon(true);
        // listenerThread.start();
        // log.info("🎧 PostgreSQL LISTEN/NOTIFY started");
    }

    @PreDestroy
    public void stopListening() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        log.info("🛑 PostgreSQL LISTEN/NOTIFY stopped");
    }

    /**
     * Background thread that listens to PostgreSQL notifications
     * DISABLED - kept for reference
     */
    @SuppressWarnings("unused")
    private void listen() {
        while (running) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Start listening
                stmt.execute("LISTEN new_notification");
                log.info("✅ Listening on channel: new_notification");

                PGConnection pgConn = conn.unwrap(PGConnection.class);

                while (running) {
                    // Wait for notifications (1 second timeout)
                    PGNotification[] notifications = pgConn.getNotifications(1000);

                    if (notifications != null && notifications.length > 0) {
                        for (PGNotification notification : notifications) {
                            handleNotification(notification.getParameter());
                        }
                    }
                }

            } catch (Exception e) {
                if (running) {
                    log.error("❌ PostgreSQL LISTEN error, will retry in 5s", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Handle incoming PostgreSQL notification
     * DISABLED - kept for reference
     */
    private void handleNotification(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);
            
            Long notificationId = json.get("id").asLong();
            Long userId = json.hasNonNull("userId") ? json.get("userId").asLong() : null;
            String title = json.get("title").asText();
            String message = json.get("message").asText();
            String type = json.get("type").asText();

            log.info("📨 Received notification: id={}, userId={}, title={}", 
                    notificationId, userId != null ? userId : "BROADCAST", title);

            // Create notification object for SSE
            Map<String, Object> notifData = Map.of(
                "id", notificationId,
                "title", title,
                "message", message,
                "type", type,
                "timestamp", json.get("createdAt").asText()
            );

            // Send to appropriate SSE clients
            if (userId == null) {
                // Broadcast to all connected clients
                sendToBroadcast(notifData);
            } else {
                // Send to specific user
                sendToUser(userId, notifData);
            }

            // Mark as delivered in database (PREVENTS DUPLICATE SENDS)
            markAsDelivered(notificationId);

        } catch (Exception e) {
            log.error("❌ Failed to handle PostgreSQL notification", e);
        }
    }

    /**
     * Send notification to all broadcast SSE connections
     */
    private void sendToBroadcast(Map<String, Object> notifData) {
        log.info("📢 Broadcasting to {} connections", broadcastConnections.size());
        
        broadcastConnections.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notifData));
                return false; // Keep emitter
            } catch (Exception e) {
                log.warn("Failed to send to broadcast connection, removing");
                return true; // Remove emitter
            }
        });
    }

    /**
     * Send notification to specific user's SSE connections
     */
    private void sendToUser(Long userId, Map<String, Object> notifData) {
        CopyOnWriteArrayList<SseEmitter> emitters = userConnections.get(userId);
        
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE connections for user {}, notification will be caught by Quartz fallback", userId);
            return;
        }

        log.info("📬 Sending to user {} ({} connections)", userId, emitters.size());

        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notifData));
                return false; // Keep emitter
            } catch (Exception e) {
                log.warn("Failed to send to user {} connection, removing", userId);
                return true; // Remove emitter
            }
        });

        if (emitters.isEmpty()) {
            userConnections.remove(userId);
        }
    }

    /**
     * Mark notification as delivered to prevent re-sending
     */
    private void markAsDelivered(Long notificationId) {
        try {
            notificationRepository.findById(notificationId).ifPresent(notif -> {
                notif.markAsDelivered();
                notificationRepository.save(notif);
                log.debug("✅ Marked notification {} as delivered", notificationId);
            });
        } catch (Exception e) {
            log.error("Failed to mark notification {} as delivered", notificationId, e);
        }
    }

    /**
     * Register SSE connection for a user
     * @deprecated Use SseNotificationDeliveryService instead
     */
    @Deprecated
    public void registerUserConnection(Long userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("✅ Registered SSE connection for user {}", userId);

        // Remove on completion/timeout
        emitter.onCompletion(() -> removeUserConnection(userId, emitter));
        emitter.onTimeout(() -> removeUserConnection(userId, emitter));
        emitter.onError(e -> removeUserConnection(userId, emitter));
    }

    /**
     * Register SSE connection for broadcast notifications
     * @deprecated Use SseNotificationDeliveryService instead
     */
    @Deprecated
    public void registerBroadcastConnection(SseEmitter emitter) {
        broadcastConnections.add(emitter);
        log.info("✅ Registered broadcast SSE connection (total: {})", broadcastConnections.size());

        emitter.onCompletion(() -> broadcastConnections.remove(emitter));
        emitter.onTimeout(() -> broadcastConnections.remove(emitter));
        emitter.onError(e -> broadcastConnections.remove(emitter));
    }

    private void removeUserConnection(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = userConnections.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userConnections.remove(userId);
            }
        }
        log.info("🔌 Removed SSE connection for user {}", userId);
    }
}
