-- Allow re-adding a credential with the same provider+label after soft-revoke

ALTER TABLE user_ai_credentials DROP CONSTRAINT IF EXISTS uq_user_ai_credentials_user_provider_label;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_ai_credentials_user_provider_label_active
    ON user_ai_credentials (user_id, provider, label)
    WHERE status <> 'REVOKED';
