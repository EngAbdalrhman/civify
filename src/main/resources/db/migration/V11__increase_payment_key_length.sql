ALTER TABLE payment_transactions ALTER COLUMN payment_key TYPE TEXT;
ALTER TABLE user_subscriptions ALTER COLUMN payment_transaction_id TYPE TEXT;
