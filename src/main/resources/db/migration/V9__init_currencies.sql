-- Initialize supported currencies with exchange rates
-- This runs after Hibernate creates the tables

INSERT INTO currencies (code, name, symbol, rate_to_usd, country_codes, is_default, active, created_at, updated_at, is_deleted)
SELECT 'USD', 'US Dollar', '$', 1.0, '["US", "EC", "SV", "PA"]'::jsonb, TRUE, TRUE, NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'USD');

INSERT INTO currencies (code, name, symbol, rate_to_usd, country_codes, is_default, active, created_at, updated_at, is_deleted)
SELECT 'EGP', 'Egyptian Pound', 'E£', 50.0, '["EG"]'::jsonb, FALSE, TRUE, NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'EGP');

INSERT INTO currencies (code, name, symbol, rate_to_usd, country_codes, is_default, active, created_at, updated_at, is_deleted)
SELECT 'SAR', 'Saudi Riyal', 'ر.س', 3.75, '["SA"]'::jsonb, FALSE, TRUE, NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'SAR');

INSERT INTO currencies (code, name, symbol, rate_to_usd, country_codes, is_default, active, created_at, updated_at, is_deleted)
SELECT 'AED', 'UAE Dirham', 'د.إ', 3.67, '["AE"]'::jsonb, FALSE, TRUE, NOW(), NOW(), false
WHERE NOT EXISTS (SELECT 1 FROM currencies WHERE code = 'AED');
