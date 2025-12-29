-- Add missing columns to promo_code table if they don't exist
ALTER TABLE promo_code ADD COLUMN IF NOT EXISTS applicability_type VARCHAR(20) DEFAULT 'ALL' NOT NULL;
ALTER TABLE promo_code ADD COLUMN IF NOT EXISTS max_discount_amount DECIMAL(19,2);

-- Create token_purchases table
CREATE TABLE IF NOT EXISTS token_purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    promo_code_id BIGINT,
    amount DECIMAL(19,2) NOT NULL,
    tokens INTEGER NOT NULL,
    payment_transaction_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT,
    
    CONSTRAINT fk_token_purchase_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_token_purchase_package FOREIGN KEY (package_id) REFERENCES token_packages(id),
    CONSTRAINT fk_token_purchase_promo FOREIGN KEY (promo_code_id) REFERENCES promo_code(id)
);

-- Index for payment lookups to ensure fast webhooks
CREATE INDEX IF NOT EXISTS idx_token_purchase_payment_ref ON token_purchases(payment_transaction_id);
