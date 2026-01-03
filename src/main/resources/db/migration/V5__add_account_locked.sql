-- Replace account_locked with status enum column
-- First drop old column and index if they exist
DROP INDEX IF EXISTS idx_users_account_locked;
ALTER TABLE users DROP COLUMN IF EXISTS account_locked;

-- Add new status column with enum values
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Create index for filtering by status
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
