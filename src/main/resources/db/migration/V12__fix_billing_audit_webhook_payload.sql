-- Fix billing_audit_logs.webhook_payload column type
-- The column needs to be TEXT or JSONB to store large payload data

DO $$
BEGIN
    -- 1. Alter webhook_payload to TEXT (increase size from varchar(255))
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'billing_audit_logs' 
        AND column_name = 'webhook_payload'
    ) THEN
        ALTER TABLE billing_audit_logs 
        ALTER COLUMN webhook_payload TYPE TEXT;
    END IF;

    -- 2. Alter transaction_id to TEXT to handle long IDs
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'billing_audit_logs' 
        AND column_name = 'transaction_id'
        AND data_type = 'character varying'
        AND character_maximum_length < 1000
    ) THEN
        ALTER TABLE billing_audit_logs 
        ALTER COLUMN transaction_id TYPE TEXT;
    END IF;

    -- 3. Attempt to convert webhook_payload to JSONB
    -- We use a safe cast or keep as TEXT if conversion isn't straightforward
    -- Since the Entity defines it as JSONB, we should try to make it JSONB.
    BEGIN
        ALTER TABLE billing_audit_logs 
        ALTER COLUMN webhook_payload TYPE JSONB USING webhook_payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        -- If conversion fails (invalid json), keep as TEXT 
        -- (Hibernate might complain if it expects JSONB but gets TEXT, but typically it works if it's just reading/writing string)
        -- Alternatively, set invalid entries to null (lossy but compliant)
        -- For now, we assume data will be valid JSON or empty.
        RAISE NOTICE 'Could not convert webhook_payload to JSONB, leaving as TEXT: %', SQLERRM;
    END;

END $$;
