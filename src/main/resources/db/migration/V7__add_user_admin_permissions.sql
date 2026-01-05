-- =====================================================
-- Add Granular User Admin Permissions
-- =====================================================
-- New permissions for fine-grained user management:
-- USER.MODIFY_GROUP - Change user's permission group
-- USER.MODIFY_LIMITS - Update user's CV limit override
-- USER.MODIFY_TOKENS - Update user's extra tokens wallet
-- =====================================================

-- User Group Management Permission
INSERT INTO permissions (name, description, category)
SELECT 'USER.MODIFY_GROUP', 'Change User Permission Group', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'USER.MODIFY_GROUP');

-- User Limits Management Permission
INSERT INTO permissions (name, description, category)
SELECT 'USER.MODIFY_LIMITS', 'Update User CV Limit Override', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'USER.MODIFY_LIMITS');

-- User Tokens Management Permission
INSERT INTO permissions (name, description, category)
SELECT 'USER.MODIFY_TOKENS', 'Update User Extra Tokens', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'USER.MODIFY_TOKENS');

-- Assign new permissions to Super Admin group
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Super Admin' AND p.name = 'USER.MODIFY_GROUP'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Super Admin' AND p.name = 'USER.MODIFY_LIMITS'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Super Admin' AND p.name = 'USER.MODIFY_TOKENS'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);
