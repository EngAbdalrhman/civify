-- BYOK: user settings, encrypted credentials, usage audit log

CREATE TABLE IF NOT EXISTS user_ai_settings (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    byok_mode VARCHAR(32) NOT NULL DEFAULT 'DISABLED',
    fallback_chain JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_ai_settings_byok_mode_check CHECK (byok_mode IN ('DISABLED', 'OPPORTUNISTIC', 'STRICT'))
);

CREATE TABLE IF NOT EXISTS user_ai_credentials (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    label VARCHAR(64) NOT NULL,
    ciphertext BYTEA NOT NULL,
    iv BYTEA NOT NULL,
    key_fingerprint VARCHAR(64) NOT NULL,
    default_model VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    last_validated_at TIMESTAMP,
    last_error VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_ai_credentials_provider_check CHECK (provider IN ('OPENAI', 'ANTHROPIC', 'OPENROUTER', 'GEMINI', 'VERTEX')),
    CONSTRAINT user_ai_credentials_status_check CHECK (status IN ('ACTIVE', 'REVOKED', 'INVALID')),
    CONSTRAINT uq_user_ai_credentials_user_provider_label UNIQUE (user_id, provider, label)
);

CREATE INDEX IF NOT EXISTS idx_user_ai_credentials_user_status ON user_ai_credentials(user_id, status);

CREATE TABLE IF NOT EXISTS ai_credential_usage_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT,
    credential_id UUID REFERENCES user_ai_credentials(id) ON DELETE SET NULL,
    source VARCHAR(16) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(128) NOT NULL,
    token_usage_type VARCHAR(30) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    latency_ms INT NOT NULL DEFAULT 0,
    success BOOLEAN NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ai_credential_usage_log_source_check CHECK (source IN ('USER_BYOK', 'ORG_BYOK', 'PLATFORM'))
);

CREATE INDEX IF NOT EXISTS idx_ai_cred_usage_user_created ON ai_credential_usage_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_cred_usage_org_created ON ai_credential_usage_log(organization_id, created_at DESC)
    WHERE organization_id IS NOT NULL;
