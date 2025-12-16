package com.app.civify.service.notification;

import com.app.civify.entity.DeviceToken;
import com.app.civify.repository.DeviceTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Firebase Cloud Messaging service for sending push notifications.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Send notification to specific user (all their devices).
     */
    @Async
    @Transactional(readOnly = true)
    public void sendToUser(Long userId, String title, String message) {
        if (!isFirebaseInitialized()) {
            log.warn("Firebase not initialized. Skipping FCM notification");
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (tokens.isEmpty()) {
            log.info("No active device tokens for user {}", userId);
            return;
        }

        sendToTokens(tokens.stream().map(DeviceToken::getToken).collect(Collectors.toList()), 
                    title, message);
    }

    /**
     * Send notification to all users (broadcast).
     */
    @Async
    @Transactional(readOnly = true)
    public void sendToAll(String title, String message) {
        if (!isFirebaseInitialized()) {
            log.warn("Firebase not initialized. Skipping FCM broadcast");
            return;
        }

        List<DeviceToken> allTokens = deviceTokenRepository.findAllActiveTokens();
        
        if (allTokens.isEmpty()) {
            log.info("No active device tokens for broadcast");
            return;
        }

        log.info("📢 Broadcasting FCM notification to {} devices", allTokens.size());
        
        // FCM allows max 500 tokens per batch
        List<String> tokens = allTokens.stream()
                .map(DeviceToken::getToken)
                .collect(Collectors.toList());
        
        sendToTokens(tokens, title, message);
    }

    /**
     * Send notification to a list of device tokens.
     */
    private void sendToTokens(List<String> tokens, String title, String messageText) {
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(messageText)
                            .build())
                    .addAllTokens(tokens)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);

            log.info("✅ FCM sent: {} successful, {} failed out of {}", 
                    response.getSuccessCount(), 
                    response.getFailureCount(),
                    tokens.size());

            // Handle failed tokens (mark as inactive)
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String errorCode = responses.get(i).getException() != null 
                                ? responses.get(i).getException().getMessagingErrorCode() != null
                                        ? responses.get(i).getException().getMessagingErrorCode().name()
                                        : "UNKNOWN"
                                : "UNKNOWN";
                        
                        // Deactivate invalid tokens
                        if ("INVALID_ARGUMENT".equals(errorCode) || "UNREGISTERED".equals(errorCode)) {
                            deviceTokenRepository.deactivateToken(tokens.get(i));
                            log.warn("Deactivated invalid FCM token: {}", errorCode);
                        }
                    }
                }
            }

        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM send failed: {}", e.getMessage());
        }
    }

    /**
     * Check if Firebase is initialized
     */
    private boolean isFirebaseInitialized() {
        return !FirebaseApp.getApps().isEmpty();
    }
}
