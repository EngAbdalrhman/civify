-- Add saved card token to user_subscriptions for auto-renewal
ALTER TABLE user_subscriptions ADD COLUMN saved_card_token VARCHAR(512);
ALTER TABLE user_subscriptions ADD COLUMN card_masked_pan VARCHAR(30);
ALTER TABLE user_subscriptions ADD COLUMN card_type VARCHAR(20);

-- Track auto-charge attempts
ALTER TABLE user_subscriptions ADD COLUMN last_auto_charge_attempt TIMESTAMP;
ALTER TABLE user_subscriptions ADD COLUMN auto_charge_failure_count INTEGER DEFAULT 0;

-- Add card token reference to payment_transactions (which token was used)
ALTER TABLE payment_transactions ADD COLUMN saved_card_token VARCHAR(512);
