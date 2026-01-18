package com.app.civify.service.notification;

import com.app.civify.service.notification.dto.NotificationMessage;

/**
 * Interface for publishing notifications to a message queue/bus.
 */
public interface NotificationPublisher {
    
    /**
     * Publish a notification message
     */
    void publish(NotificationMessage message);
}
