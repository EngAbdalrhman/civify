package com.app.civify.service.notification.webpush;

import com.app.civify.entity.PushSubscription;
import com.app.civify.repository.PushSubscriptionRepository;
import com.app.civify.service.notification.dto.NotificationMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;

/**
 * Service for sending Web Push notifications via VAPID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final PushSubscriptionRepository subscriptionRepository;

    @Value("${webpush.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${webpush.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${webpush.vapid.subject:mailto:support@civify.cv}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (vapidPublicKey == null || vapidPublicKey.isEmpty() || 
            vapidPrivateKey == null || vapidPrivateKey.isEmpty()) {
            log.warn("⚠️ Web Push VAPID keys not configured. Web Push notifications disabled.");
            return;
        }

        try {
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            log.info("✅ Web Push service initialized with VAPID keys");
        } catch (Exception e) {
            log.error("❌ Failed to initialize Web Push service", e);
        }
    }

    /**
     * Send push notification to all active subscriptions for a user
     */
    @Async
    public void sendPushNotification(Long userId, NotificationMessage message) {
        if (pushService == null) {
            log.debug("Web Push service not initialized, skipping push notification");
            return;
        }

        List<PushSubscription> subscriptions = subscriptionRepository.findActiveByUserId(userId);

        if (subscriptions.isEmpty()) {
            log.debug("No active push subscriptions for user {}", userId);
            return;
        }

        log.info("📱 Sending Web Push to {} subscription(s) for user {}", subscriptions.size(), userId);

        String payload = buildPayload(message);

        for (PushSubscription sub : subscriptions) {
            try {
                Subscription subscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dhKey(), sub.getAuthKey())
                );

                Notification notification = new Notification(subscription, payload);
                pushService.send(notification);

                sub.markAsUsed();
                subscriptionRepository.save(sub);

                log.debug("✅ Sent Web Push to subscription {}", sub.getId());

            } catch (Exception e) {
                log.warn("❌ Failed to send Web Push to subscription {}: {}", sub.getId(), e.getMessage());
                
                // Deactivate subscription if endpoint is gone (410 Gone)
                if (e.getMessage() != null && e.getMessage().contains("410")) {
                    sub.deactivate();
                    subscriptionRepository.save(sub);
                    log.info("🔌 Deactivated stale push subscription {}", sub.getId());
                }
            }
        }
    }

    private String buildPayload(NotificationMessage message) {
        return String.format(
            "{\"title\":\"%s\",\"body\":\"%s\",\"type\":\"%s\",\"id\":%d}",
            escapeJson(message.getTitle()),
            escapeJson(message.getMessage()),
            message.getType(),
            message.getId()
        );
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Get VAPID public key for frontend subscription
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Check if Web Push is enabled
     */
    public boolean isEnabled() {
        return pushService != null;
    }
}
