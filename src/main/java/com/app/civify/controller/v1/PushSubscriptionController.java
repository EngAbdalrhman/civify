package com.app.civify.controller.v1;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.entity.PushSubscription;
import com.app.civify.entity.User;
import com.app.civify.repository.PushSubscriptionRepository;
import com.app.civify.repository.UserRepository;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.notification.webpush.WebPushService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for managing Web Push subscriptions.
 */
@ApiV1Controller
@RequestMapping("/push-subscriptions")
@Slf4j
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final WebPushService webPushService;

    /**
     * Get VAPID public key for frontend to create subscription
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<Map<String, Object>> getVapidKey() {
        return ResponseEntity.ok(Map.of(
            "publicKey", webPushService.getVapidPublicKey(),
            "enabled", webPushService.isEnabled()
        ));
    }

    /**
     * Subscribe to push notifications
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, String>> subscribe(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody SubscriptionRequest request) {

        if (!webPushService.isEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Web Push not enabled"));
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if subscription already exists
        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(request.getEndpoint());
        if (existing.isPresent()) {
            PushSubscription sub = existing.get();
            // Update keys if they changed
            sub.setP256dhKey(request.getP256dh());
            sub.setAuthKey(request.getAuth());
            sub.setActive(true);
            subscriptionRepository.save(sub);
            log.info("✅ Updated push subscription for user {}", user.getId());
            return ResponseEntity.ok(Map.of("status", "updated"));
        }

        // Create new subscription
        PushSubscription subscription = PushSubscription.builder()
                .user(user)
                .endpoint(request.getEndpoint())
                .p256dhKey(request.getP256dh())
                .authKey(request.getAuth())
                .userAgent(request.getUserAgent())
                .build();

        subscriptionRepository.save(subscription);
        log.info("✅ Created push subscription for user {}", user.getId());

        return ResponseEntity.ok(Map.of("status", "subscribed"));
    }

    /**
     * Unsubscribe from push notifications
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<Map<String, String>> unsubscribe(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UnsubscribeRequest request) {

        subscriptionRepository.deleteByEndpoint(request.getEndpoint());
        log.info("🔌 Removed push subscription for user {}", userPrincipal.getId());

        return ResponseEntity.ok(Map.of("status", "unsubscribed"));
    }

    @Data
    public static class SubscriptionRequest {
        private String endpoint;
        private String p256dh;
        private String auth;
        private String userAgent;
    }

    @Data
    public static class UnsubscribeRequest {
        private String endpoint;
    }
}
