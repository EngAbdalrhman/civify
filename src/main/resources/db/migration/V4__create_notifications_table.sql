-- Notifications table for PostgreSQL-based message queue
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE, -- NULL = broadcast to all users
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- INFO, SUCCESS, WARNING, ERROR, PROMO, SYSTEM
    
    -- Queue/Processing management
    delivered BOOLEAN DEFAULT FALSE,
    delivered_at TIMESTAMP,
    delivery_attempts INTEGER DEFAULT 0,
    last_attempt_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_for TIMESTAMP, -- For delayed notifications (future use)
    
    -- Metadata
    metadata JSONB, -- Extra data like action URL, image URL, etc.
    
    -- Deduplication
    idempotency_key VARCHAR(255) -- Prevent duplicate notifications
);

-- Indexes for performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_delivered ON notifications(delivered) WHERE delivered = FALSE;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_idempotency_key ON notifications(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Trigger function to send pg_notify on new notifications
CREATE OR REPLACE FUNCTION notify_new_notification()
RETURNS TRIGGER AS $$
BEGIN
    -- Only notify for undelivered notifications
    IF NOT NEW.delivered THEN
        PERFORM pg_notify(
            'new_notification',
            json_build_object(
                'id', NEW.id,
                'userId', NEW.user_id,
                'title', NEW.title,
                'message', NEW.message,
                'type', NEW.type,
                'createdAt', NEW.created_at,
                'metadata', NEW.metadata
            )::text
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to fire on INSERT
CREATE TRIGGER notification_insert_trigger
AFTER INSERT ON notifications
FOR EACH ROW
EXECUTE FUNCTION notify_new_notification();

-- Comments for documentation
COMMENT ON TABLE notifications IS 'Notification queue - PostgreSQL-based message system';
COMMENT ON COLUMN notifications.user_id IS 'NULL = broadcast notification to all users';
COMMENT ON COLUMN notifications.delivered IS 'TRUE = notification sent to client via SSE/WebSocket';
COMMENT ON COLUMN notifications.idempotency_key IS 'Unique key to prevent duplicate notifications (e.g., payment_123_success)';
