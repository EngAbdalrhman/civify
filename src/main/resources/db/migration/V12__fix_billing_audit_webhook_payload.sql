-- Fix billing_audit_logs.webhook_payload column type
-- The column needs to be TEXT or JSONB to store large payload data

-- First, drop the column if it exists with wrong type and recreate as JSONB
-- Or just alter if already JSONB but constraints are wrong

-- Check if column exists and alter to JSONB type
DO $$
BEGIN
    -- If the column is VARCHAR, alter to JSONB
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'billing_audit_logs' 
        AND column_name = 'webhook_payload'
        AND data_type = 'character varying'
    ) THEN
        -- First convert to TEXT, then to JSONB (handles the type change)
        ALTER TABLE billing_audit_logs 
        ALTER COLUMN webhook_payload TYPE TEXT;
        
        ALTER TABLE billing_audit_logs 
        ALTER COLUMN webhook_payload TYPE JSONB USING webhook_payload::jsonb;
    END IF;
    
    -- If column doesn't exist at all, create it
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'billing_audit_logs' 
        AND column_name = 'webhook_payload'
    ) THEN
        ALTER TABLE billing_audit_logs 
        ADD COLUMN webhook_payload JSONB;
    END IF;
END $$;
