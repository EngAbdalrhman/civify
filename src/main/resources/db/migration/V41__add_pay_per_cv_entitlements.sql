-- V41: Add Pay-Per-CV Entitlements and Product Support

CREATE TABLE IF NOT EXISTS cv_entitlements (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id              VARCHAR(36) REFERENCES resumes(id) ON DELETE SET NULL,
    transaction_id         VARCHAR(36) REFERENCES billing_transactions(id) ON DELETE SET NULL,
    order_id               VARCHAR(255),
    entitlement_type       VARCHAR(50) NOT NULL DEFAULT 'SINGLE_CV',
    credits_remaining      INT NOT NULL DEFAULT 1,
    watermark_free         BOOLEAN NOT NULL DEFAULT TRUE,
    allow_doc_export       BOOLEAN NOT NULL DEFAULT TRUE,
    unlimited_edits_until  TIMESTAMP WITH TIME ZONE NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cv_entitlements_user_resume ON cv_entitlements(user_id, resume_id);
CREATE INDEX IF NOT EXISTS idx_cv_entitlements_user_status ON cv_entitlements(user_id, status);
CREATE INDEX IF NOT EXISTS idx_cv_entitlements_order_id ON cv_entitlements(order_id);
