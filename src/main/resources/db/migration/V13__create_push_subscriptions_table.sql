-- Push Subscriptions table for Web Push notifications
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint VARCHAR(500) NOT NULL UNIQUE,
    p256dh_key VARCHAR(500) NOT NULL,
    auth_key VARCHAR(500) NOT NULL,
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Index for faster lookups by user
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user_id ON push_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_active ON push_subscriptions(user_id, active);
