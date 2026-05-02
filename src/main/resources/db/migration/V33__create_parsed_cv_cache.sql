CREATE TABLE parsed_cv_cache (
    id           BIGSERIAL PRIMARY KEY,
    file_hash    VARCHAR(64) NOT NULL UNIQUE,
    parsed_json  TEXT        NOT NULL,
    detected_language VARCHAR(10),
    confidence   DOUBLE PRECISION,
    processing_method VARCHAR(50),
    tokens_used  INT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX idx_parsed_cv_cache_hash ON parsed_cv_cache(file_hash);
