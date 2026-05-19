-- ============================================================================
-- V36: Add Dodo Payments support
-- - Add dodo_product_id column to subscription_plans
-- - Create currency_conversion_rates table for multi-gateway pricing
-- - Seed default USD→EGP conversion rate
-- ============================================================================

-- 1) Map subscription plans to Dodo Payments products
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS dodo_product_id VARCHAR(255);

COMMENT ON COLUMN subscription_plans.dodo_product_id
    IS 'Dodo Payments product ID (e.g. pdt_xxx). NULL = not available via Dodo.';

-- 2) Static currency conversion rates (managed manually in DB for now)
CREATE TABLE IF NOT EXISTS currency_conversion_rates (
    id              BIGSERIAL PRIMARY KEY,
    from_currency   VARCHAR(3) NOT NULL,
    to_currency     VARCHAR(3) NOT NULL,
    rate            DECIMAL(18,8) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_currency_pair UNIQUE (from_currency, to_currency)
);

-- 3) Seed the default USD→EGP rate (update this value to match current market rate)
INSERT INTO currency_conversion_rates (from_currency, to_currency, rate, created_by)
VALUES ('USD', 'EGP', 50.00000000, 'system')
ON CONFLICT ON CONSTRAINT uq_currency_pair DO NOTHING;
