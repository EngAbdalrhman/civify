-- Grant CV.CUSTOM_WATERMARK to Super Admin and to groups linked to plans with CUSTOM_BRANDING enabled

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
  AND p.name = 'CV.CUSTOM_WATERMARK'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

--INSERT INTO group_permissions (group_id, permission_id)
--SELECT DISTINCT sp.linked_group_id, p.id
--FROM subscription_plans sp
--JOIN plan_feature_limits pfl ON pfl.plan_id = sp.id
--JOIN plan_features pf ON pf.id = pfl.feature_id
--JOIN permissions p ON p.name = 'CV.CUSTOM_WATERMARK'
--WHERE pf.feature_type = 'CUSTOM_BRANDING'
--  AND pfl.enabled = true
--  AND sp.linked_group_id IS NOT NULL
--  AND NOT EXISTS (
--    SELECT 1 FROM group_permissions gp
--    WHERE gp.group_id = sp.linked_group_id AND gp.permission_id = p.id
--);
