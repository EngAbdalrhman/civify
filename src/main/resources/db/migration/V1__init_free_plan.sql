-- =====================================================
-- Free Plan Initialization Script
-- =====================================================
-- This script initializes the database with plan features
-- and a free plan. It's designed to be idempotent - safe
-- to run multiple times.
--
-- NOTE: This script is for reference/manual use only.
-- The application automatically initializes data via
-- PlanDataInitializer.java on startup.
-- =====================================================

-- =====================================================
-- 1. Initialize Plan Features
-- =====================================================

-- Insert all plan features if they don't exist
INSERT INTO plan_features (id, feature_type, name, description, has_limit, unit, active, display_order, created_at, updated_at, is_deleted)
SELECT * FROM (
    SELECT 1 as id, 'CV_GENERATION' as feature_type, 'CV Generation' as name, 'Generate tailored CVs' as description, true as has_limit, 'CVs' as unit, true as active, 0 as display_order, CURRENT_TIMESTAMP as created_at, CURRENT_TIMESTAMP as updated_at, false as is_deleted
    UNION ALL
    SELECT 2, 'API_ACCESS', 'API Access', 'Access to REST API', true, 'API calls', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 3, 'WATERMARK_REMOVAL', 'Watermark Removal', 'Remove watermark from generated CVs', false, null, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 4, 'DASHBOARD_ACCESS', 'Dashboard Access', 'Access to analytics dashboard', false, null, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 5, 'PREMIUM_TEMPLATES', 'Premium Templates', 'Access to premium CV templates', false, null, true, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 6, 'AI_MODEL_ACCESS', 'AI Model Access', 'Access to advanced AI models', false, null, true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 7, 'PRIORITY_SUPPORT', 'Priority Support', '24/7 priority customer support', false, null, true, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 8, 'CUSTOM_BRANDING', 'Custom Branding', 'Add your own branding to CVs', false, null, true, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 9, 'EXPORT_FORMATS', 'Export Formats', 'Export CVs in multiple formats', false, null, true, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 10, 'TEAM_COLLABORATION', 'Team Collaboration', 'Collaborate with team members', true, 'users', true, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 11, 'STORAGE_SPACE', 'Storage Space', 'Cloud storage for CVs and documents', true, 'GB', true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
    UNION ALL
    SELECT 12, 'INTEGRATION_ACCESS', 'Integration Access', 'Access to third-party integrations', false, null, true, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
) AS features
WHERE NOT EXISTS (
    SELECT 1 FROM plan_features WHERE feature_type = features.feature_type
);

-- =====================================================
-- 2. Initialize Free Plan
-- =====================================================

-- Insert free plan if it doesn't exist
INSERT INTO subscription_plans (id, name, description, details, cv_limit, api_limit, watermark, monthly_price, annual_price, active, created_at, updated_at, is_deleted)
SELECT 
    1 as id,
    'free' as name,
    'Free plan with basic features' as description,
    'Get started with our free plan including limited CV generation, API access, and basic export formats.' as details,
    5 as cv_limit,
    10 as api_limit,
    true as watermark,
    0.00 as monthly_price,
    0.00 as annual_price,
    true as active,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at,
    false as is_deleted
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE name = 'free'
);

-- =====================================================
-- 3. Initialize Free Plan Feature Limits
-- =====================================================

-- Add feature limits to free plan
-- NOTE: This assumes the free plan has id=1 and features have sequential IDs 1-12
-- Adjust IDs if your database auto-increment starts differently

-- CV Generation: 5 CVs/month
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'CV_GENERATION'), 5, true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'CV_GENERATION')
);

-- API Access: 10 calls/month
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'API_ACCESS'), 10, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'API_ACCESS')
);

-- Watermark Removal: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'WATERMARK_REMOVAL'), null, false, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'WATERMARK_REMOVAL')
);

-- Dashboard Access: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'DASHBOARD_ACCESS'), null, false, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'DASHBOARD_ACCESS')
);

-- Premium Templates: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'PREMIUM_TEMPLATES'), null, false, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'PREMIUM_TEMPLATES')
);

-- Export Formats: Enabled (PDF)
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'EXPORT_FORMATS'), null, true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'EXPORT_FORMATS')
);

-- AI Model Access: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'AI_MODEL_ACCESS'), null, false, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'AI_MODEL_ACCESS')
);

-- Priority Support: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'PRIORITY_SUPPORT'), null, false, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'PRIORITY_SUPPORT')
);

-- Custom Branding: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'CUSTOM_BRANDING'), null, false, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'CUSTOM_BRANDING')
);

-- Team Collaboration: Disabled (0 users)
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'TEAM_COLLABORATION'), 0, false, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'TEAM_COLLABORATION')
);

-- Storage Space: 1 GB
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'STORAGE_SPACE'), 1, true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'STORAGE_SPACE')
);

-- Integration Access: Disabled
INSERT INTO plan_feature_limits (plan_id, feature_id, limit_value, enabled, display_order, created_at, updated_at, is_deleted)
SELECT 1, (SELECT id FROM plan_features WHERE feature_type = 'INTEGRATION_ACCESS'), null, false, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_limits 
    WHERE plan_id = 1 AND feature_id = (SELECT id FROM plan_features WHERE feature_type = 'INTEGRATION_ACCESS')
);

-- =====================================================
-- 4. Initialize Free Plan Pricing Options
-- =====================================================

-- Monthly pricing: $0.00
INSERT INTO plan_pricing (plan_id, billing_cycle, price, original_price, discount_percentage, currency, active, display_order, recommended, created_at, updated_at, is_deleted)
SELECT 
    1 as plan_id,
    'MONTHLY' as billing_cycle,
    0.00 as price,
    0.00 as original_price,
    0.00 as discount_percentage,
    'USD' as currency,
    true as active,
    0 as display_order,
    false as recommended,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at,
    false as is_deleted
WHERE NOT EXISTS (
    SELECT 1 FROM plan_pricing WHERE plan_id = 1 AND billing_cycle = 'MONTHLY'
);

-- Annual pricing: $0.00
INSERT INTO plan_pricing (plan_id, billing_cycle, price, original_price, discount_percentage, currency, active, display_order, recommended, created_at, updated_at, is_deleted)
SELECT 
    1 as plan_id,
    'ANNUAL' as billing_cycle,
    0.00 as price,
    0.00 as original_price,
    0.00 as discount_percentage,
    'USD' as currency,
    true as active,
    1 as display_order,
    true as recommended,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at,
    false as is_deleted
WHERE NOT EXISTS (
    SELECT 1 FROM plan_pricing WHERE plan_id = 1 AND billing_cycle = 'ANNUAL'
);

-- =====================================================
-- Verification Queries
-- =====================================================
-- Uncomment to verify the initialization:
/*
SELECT * FROM plan_features ORDER BY display_order;
SELECT * FROM subscription_plans WHERE name = 'free';
SELECT pfl.*, pf.name as feature_name, pf.feature_type 
FROM plan_feature_limits pfl
JOIN plan_features pf ON pfl.feature_id = pf.id
WHERE pfl.plan_id = (SELECT id FROM subscription_plans WHERE name = 'free')
ORDER BY pfl.display_order;
SELECT * FROM plan_pricing WHERE plan_id = (SELECT id FROM subscription_plans WHERE name = 'free');
*/
