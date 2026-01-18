package com.app.civify.service.notification.pubsub;

import com.app.civify.repository.NotificationRepository;
import com.app.civify.service.notification.SseNotificationDeliveryService;
import com.app.civify.service.notification.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Listens to Google Cloud Pub/Sub subscription and delivers notifications via SSE.
 * Only active when notification.provider=pubsub
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "spring.cloud.gcp.pubsub.enabled", havingValue = "true")
public class PubSubNotificationListener {

    private final SseNotificationDeliveryService sseDeliveryService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "notificationInputChannel")
    public void handleMessage(Message<String> message) {
        BasicAcknowledgeablePubsubMessage originalMessage = 
            message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
        
        try {
            String payload = message.getPayload();
            NotificationMessage notifMessage = objectMapper.readValue(payload, NotificationMessage.class);

            log.info("📨 Received notification from Pub/Sub: id={}, userId={}", 
                    notifMessage.getId(), notifMessage.getUserId() != null ? notifMessage.getUserId() : "BROADCAST");

            // Deliver via SSE to connected clients
            sseDeliveryService.deliver(notifMessage);

            // Mark as delivered in database
            markAsDelivered(notifMessage.getId());

            // Acknowledge the message
            if (originalMessage != null) {
                originalMessage.ack();
            }

        } catch (Exception e) {
            log.error("❌ Failed to process Pub/Sub message", e);
            // Nack the message for retry
            if (originalMessage != null) {
                originalMessage.nack();
            }
        }
    }

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
}
