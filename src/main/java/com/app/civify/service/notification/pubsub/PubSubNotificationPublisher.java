package com.app.civify.service.notification.pubsub;

import com.app.civify.service.notification.NotificationPublisher;
import com.app.civify.service.notification.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publishes notification messages to Google Cloud Pub/Sub topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.provider", havingValue = "pubsub")
public class PubSubNotificationPublisher implements NotificationPublisher {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gcp.pubsub.topic-name}")
    private String topicName;

    /**
     * Publish notification to Pub/Sub topic
     */
    @Override
    public void publish(NotificationMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            pubSubTemplate.publish(topicName, payload)
                .whenComplete((messageId, ex) -> {
                    if (ex != null) {
                        log.error("❌ Failed to publish notification {} to Pub/Sub", message.getId(), ex);
                    } else {
                        log.info("📤 Published notification to Pub/Sub: id={}, messageId={}", message.getId(), messageId);
                    }
                });
        } catch (Exception e) {
            log.error("❌ Failed to serialize notification for Pub/Sub", e);
        }
    }
}
