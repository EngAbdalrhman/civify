package com.app.civify.service.notification.postgres;

import com.app.civify.service.notification.NotificationPublisher;
import com.app.civify.service.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op publisher for PostgreSQL mode. In Postgres mode, notifications are
 * handled by database triggers and the Listener, so we don't need to explicitly
 * "publish" anything in Java code.
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "notification.publisher", havingValue = "postgres")
public class PostgresNotificationPublisher implements NotificationPublisher {

    @Override
    public void publish(NotificationMessage message) {
        log.debug("📝 Notification saved to DB (Postgres Listener will pick it up): id={}", message.getId());
    }
}
