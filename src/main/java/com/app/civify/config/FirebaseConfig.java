package com.app.civify.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Cloud Messaging configuration.
 * Initializes Firebase Admin SDK for sending push notifications.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path:#{null}}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            if (credentialsPath == null || credentialsPath.isBlank()) {
                log.warn("🔥 Firebase credentials path not configured. FCM notifications disabled.");
                log.warn("Set 'firebase.credentials.path' in application.properties to enable FCM");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK initialized successfully");
            }

        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase: {}", e.getMessage());
            log.error("FCM push notifications will NOT work until Firebase is properly configured");
        }
    }
}
