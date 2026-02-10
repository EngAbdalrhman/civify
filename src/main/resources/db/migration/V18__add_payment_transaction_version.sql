-- Add optimistic locking version column to payment_transactions
-- Prevents race conditions between webhook and redirect callbacks
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
