-- Platform watermark settings (singleton)
CREATE TABLE IF NOT EXISTS platform_watermark_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    logo_storage_key VARCHAR(255),
    opacity DOUBLE PRECISION NOT NULL DEFAULT 0.12,
    scale DOUBLE PRECISION NOT NULL DEFAULT 0.35,
    position VARCHAR(32) NOT NULL DEFAULT 'center',
    rotation DOUBLE PRECISION DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT platform_watermark_settings_singleton CHECK (id = 1)
);

INSERT INTO platform_watermark_settings (id, enabled)
SELECT 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM platform_watermark_settings WHERE id = 1);

-- Per-user custom watermark settings
CREATE TABLE IF NOT EXISTS user_watermark_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    logo_storage_key VARCHAR(255),
    opacity DOUBLE PRECISION NOT NULL DEFAULT 0.12,
    scale DOUBLE PRECISION NOT NULL DEFAULT 0.35,
    position VARCHAR(32) NOT NULL DEFAULT 'center',
    rotation DOUBLE PRECISION DEFAULT 0,
    use_platform_watermark BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Custom watermark permission
INSERT INTO permissions (name, description, category)
SELECT 'CV.CUSTOM_WATERMARK', 'Use custom watermark on PDF exports', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.CUSTOM_WATERMARK');
