-- V42: Add API Key Scopes, Expiration, and Description for Personal Access Tokens (PATs)

ALTER TABLE api_keys
    ADD COLUMN IF NOT EXISTS description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'PERSONAL_ACCESS_TOKEN';

CREATE TABLE IF NOT EXISTS api_key_scopes (
    api_key_id BIGINT NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    scope VARCHAR(50) NOT NULL,
    PRIMARY KEY (api_key_id, scope)
);

CREATE INDEX IF NOT EXISTS idx_api_key_scopes_key_id ON api_key_scopes(api_key_id);
