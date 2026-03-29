package com.app.civify.service.notification;

import com.app.civify.service.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing SSE connections and delivering notifications to connected clients.
 * This is the shared delivery service used by both Pub/Sub and fallback processors.
 */
@Service
@Slf4j
public class SseNotificationDeliveryService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<SseEmitter> broadcastConnections = new CopyOnWriteArrayList<>();

    /**
     * Deliver notification to appropriate SSE clients.
     * Returns true if at least one client received the notification.
     */
    public boolean deliver(NotificationMessage message) {
        Map<String, Object> notifData = Map.of(
            "id", message.getId(),
            "title", message.getTitle(),
            "message", message.getMessage(),
            "type", message.getType(),
            "timestamp", message.getCreatedAt()
        );

        if (message.getUserId() == null) {
            // Broadcast: send to all connected authenticated users via their user emitters
            return sendBroadcastToAllUsers(notifData);
        } else {
            return sendToUser(message.getUserId(), notifData);
        }
    }

    /**
     * Send broadcast notification to ALL authenticated users via their user-specific emitters.
     * This avoids the double-delivery that occurred when users were also in broadcastConnections.
     */
    private boolean sendBroadcastToAllUsers(Map<String, Object> notifData) {
        if (userConnections.isEmpty()) {
            log.debug("📢 No connected users for broadcast");
            return false;
        }
        log.info("📢 Broadcasting to {} users", userConnections.size());
        boolean[] anySuccess = {false};
        userConnections.forEach((userId, emitters) -> {
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(notifData));
                    anySuccess[0] = true;
                    return false;
                } catch (Exception e) {
                    log.warn("Failed to send broadcast to user {} connection, removing", userId);
                    return true;
                }
            });
            if (emitters.isEmpty()) {
                userConnections.remove(userId);
            }
        });
        return anySuccess[0];
    }

    /**
     * Send notification to specific user's SSE connections.
     * Returns true if at least one emitter succeeded.
     */
    private boolean sendToUser(Long userId, Map<String, Object> notifData) {
        CopyOnWriteArrayList<SseEmitter> emitters = userConnections.get(userId);

        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE connections for user {}, notification stored for later fetch", userId);
            return false;
        }

        log.info("📬 Sending to user {} ({} connections)", userId, emitters.size());
        boolean[] anySuccess = {false};

        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notifData));
                anySuccess[0] = true;
                return false;
            } catch (Exception e) {
                log.warn("Failed to send to user {} connection, removing", userId);
                return true;
            }
        });

        if (emitters.isEmpty()) {
            userConnections.remove(userId);
        }

        return anySuccess[0];
    }


    /**
     * Register SSE connection for a user
     */
    public void registerUserConnection(Long userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("✅ Registered SSE connection for user {}", userId);

        emitter.onCompletion(() -> removeUserConnection(userId, emitter));
        emitter.onTimeout(() -> removeUserConnection(userId, emitter));
        emitter.onError(e -> removeUserConnection(userId, emitter));
    }

    /**
     * Register SSE connection for broadcast notifications
     */
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

    /**
     * Check if user has any active SSE connections
     */
    public boolean hasActiveConnection(Long userId) {
        CopyOnWriteArrayList<SseEmitter> emitters = userConnections.get(userId);
        return emitters != null && !emitters.isEmpty();
    }
}
