-- Drop restrictive constraints on token_usage_logs to allow new enum values

ALTER TABLE token_usage_logs DROP CONSTRAINT IF EXISTS token_usage_logs_usage_type_check;
ALTER TABLE token_usage_logs DROP CONSTRAINT IF EXISTS token_usage_logs_source_check;
