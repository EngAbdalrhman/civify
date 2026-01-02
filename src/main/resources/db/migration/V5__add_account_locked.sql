-- Add account_locked column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for filtering by account_locked status
CREATE INDEX IF NOT EXISTS idx_users_account_locked ON users(account_locked);
