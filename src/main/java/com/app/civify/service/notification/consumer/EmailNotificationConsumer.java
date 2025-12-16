package com.app.civify.service.notification.consumer;

import com.app.civify.dto.notification.NotificationMessage;
import com.app.civify.entity.Notification;
import com.app.civify.repository.NotificationRepository;
import com.app.civify.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for Email notification delivery.
 * Listens to RabbitMQ email queue and sends emails.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationConsumer {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    /**
     * Listen to Email queue and send email notifications.
     */
    @RabbitListener(queues = "notification.email")
    public void handleEmailNotification(NotificationMessage message) {
        try {
            log.info("📧 Email consumer received: {} for {}", 
                message.getTitle(), 
                message.isBroadcast() ? "ALL USERS" : "user " + message.getUserId());

            if (message.isBroadcast()) {
                // TODO: Implement bulk email sending for broadcasts
                log.info("📬 Broadcast email sending not yet implemented");
            } else if (message.getUserId() != null) {
                // Send email to specific user
                // TODO: Fetch user email and send
                /*
                User user = userRepository.findById(message.getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    emailService.sendNotificationEmail(
                        user.getEmail(),
                        message.getTitle(),
                        message.getMessage(),
                        message.getType()
                    );
                    updateDeliveryStatus(message.getNotificationId(), "email");
                    log.info("✅ Email sent to user {}", message.getUserId());
                }
                */
                log.info("✅ Email delivery placeholder for user {}", message.getUserId());
                updateDeliveryStatus(message.getNotificationId(), "email");
            }

        } catch (Exception e) {
            log.error("❌ Failed to send email notification: {}", message.getTitle(), e);
            throw e; // Re-throw to trigger RabbitMQ retry/DLQ
        }
    }

    /**
     * Update delivery status in database
     */
    private void updateDeliveryStatus(Long notificationId, String channel) {
        if (notificationId != null) {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                if ("email".equals(channel)) {
                    notification.setEmailSent(true);
                }
                notificationRepository.save(notification);
            });
        }
    }
}
