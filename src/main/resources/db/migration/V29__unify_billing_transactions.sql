-- 1. Rename the table
ALTER TABLE payment_transactions RENAME TO billing_transactions;

-- 2. Add new columns
ALTER TABLE billing_transactions ADD COLUMN transaction_type VARCHAR(50);
ALTER TABLE billing_transactions ADD COLUMN invoice_id BIGINT;
ALTER TABLE billing_transactions ADD COLUMN token_package_id BIGINT;

-- 3. Add foreign keys
ALTER TABLE billing_transactions ADD CONSTRAINT fk_billing_tx_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL;
ALTER TABLE billing_transactions ADD CONSTRAINT fk_billing_tx_token_pkg FOREIGN KEY (token_package_id) REFERENCES token_packages(id) ON DELETE SET NULL;

-- 4. Backfill transaction_type
-- If metadata contains '"type":"TOKEN_TOPUP"', it's a token purchase
UPDATE billing_transactions 
SET transaction_type = 'TOKEN_TOPUP' 
WHERE metadata::text LIKE '%"type":"TOKEN_TOPUP"%' 
   OR metadata::text LIKE '%"type": "TOKEN_TOPUP"%';

-- If it's linked to an invoice (where invoice.payment_transaction_id matches order_id), it's a RENEWAL or initial invoice
UPDATE billing_transactions bt
SET transaction_type = 'RENEWAL',
    invoice_id = i.id
FROM invoices i
WHERE i.payment_transaction_id = bt.order_id 
  AND bt.transaction_type IS NULL;

-- Default everything else to SUBSCRIPTION (legacy initial purchases before invoices or general subscriptions)
UPDATE billing_transactions 
SET transaction_type = 'SUBSCRIPTION' 
WHERE transaction_type IS NULL;

-- 5. Make transaction_type non-nullable
ALTER TABLE billing_transactions ALTER COLUMN transaction_type SET NOT NULL;

-- 6. Add performance indexes for revenue queries and admin filtering
CREATE INDEX idx_billing_tx_type ON billing_transactions(transaction_type);
CREATE INDEX idx_billing_tx_status_completed_at ON billing_transactions(status, completed_at);
