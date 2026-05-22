-- Map token packages to Dodo Payments products (same pattern as subscription_plans)
ALTER TABLE token_packages
    ADD COLUMN IF NOT EXISTS dodo_product_id VARCHAR(255);

COMMENT ON COLUMN token_packages.dodo_product_id
    IS 'Dodo Payments product ID (e.g. pdt_xxx). NULL = not available via Dodo gateway.';
