ALTER TABLE users ADD COLUMN IF NOT EXISTS job_match_limit INTEGER;
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS job_match_limit INTEGER;
ALTER TABLE user_subscriptions ADD COLUMN IF NOT EXISTS current_job_match_count INTEGER DEFAULT 0;

-- Set existing records to 0 to avoid null pointer exceptions
UPDATE user_subscriptions SET current_job_match_count = 0 WHERE current_job_match_count IS NULL;

-- Default Free plans to limited, null (unlimited) for others
UPDATE subscription_plans SET job_match_limit = 3 WHERE price = 0 OR name ILIKE '%free%';
