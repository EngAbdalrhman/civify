-- =====================================================
-- Permission and Group Initialization Script
-- =====================================================
-- This migration seeds the RBAC system with:
-- 1. All permissions (CV, API, Admin features)
-- 2. Default group (Free User) for new registrations
-- 3. Super Admin group with all permissions
-- 4. Links free plan to default group
-- =====================================================

-- =====================================================
-- 1. Initialize Permissions
-- =====================================================

-- CV Permissions
INSERT INTO permissions (name, description, category)
SELECT 'CV.CREATE', 'Create and edit Resumes', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.CREATE');

INSERT INTO permissions (name, description, category)
SELECT 'CV.EXPORT.PDF', 'Export Resume as PDF', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.EXPORT.PDF');

INSERT INTO permissions (name, description, category)
SELECT 'CV.EXPORT.DOCX', 'Export Resume as DOCX', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.EXPORT.DOCX');

INSERT INTO permissions (name, description, category)
SELECT 'CV.NO_WATERMARK', 'Remove Watermark from exports', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.NO_WATERMARK');

-- API Permissions
INSERT INTO permissions (name, description, category)
SELECT 'API.ACCESS', 'Access Developer API', 'API'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'API.ACCESS');

-- Admin User Permissions
INSERT INTO permissions (name, description, category)
SELECT 'USER.VIEW', 'View Users List and Details', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'USER.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'USER.MODIFY', 'Modify Users (Block/Delete)', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'USER.MODIFY');

-- Admin Plan Permissions
INSERT INTO permissions (name, description, category)
SELECT 'PLAN.VIEW', 'View Subscription Plans', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'PLAN.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'PLAN.MODIFY', 'Create/Edit Subscription Plans', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'PLAN.MODIFY');

-- Admin Subscription Permissions
INSERT INTO permissions (name, description, category)
SELECT 'SUB.VIEW', 'View User Subscriptions', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'SUB.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'SUB.MODIFY', 'Modify User Subscriptions', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'SUB.MODIFY');

-- Admin Promo Permissions
INSERT INTO permissions (name, description, category)
SELECT 'PROMO.VIEW', 'View Promo Codes', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'PROMO.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'PROMO.MODIFY', 'Manage Promo Codes', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'PROMO.MODIFY');

-- Admin Group Permissions
INSERT INTO permissions (name, description, category)
SELECT 'GROUP.VIEW', 'View User Groups', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'GROUP.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'GROUP.MODIFY', 'Manage User Groups', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'GROUP.MODIFY');

-- Admin System Permissions
INSERT INTO permissions (name, description, category)
SELECT 'SYSTEM.LOGS.VIEW', 'View System Logs', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'SYSTEM.LOGS.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'NOTIF.SEND', 'Send Push Notifications', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'NOTIF.SEND');

INSERT INTO permissions (name, description, category)
SELECT 'NOTIF.RECEIVE', 'Receive Notifications', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'NOTIF.RECEIVE');

-- User Subscription Permissions
INSERT INTO permissions (name, description, category)
SELECT 'SUBSCRIPTION.VIEW', 'View Own Subscription Details', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'SUBSCRIPTION.VIEW');

INSERT INTO permissions (name, description, category)
SELECT 'SUBSCRIPTION.MODIFY', 'Modify User Subscriptions (Admin)', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'SUBSCRIPTION.MODIFY');

-- Additional CV Permissions
INSERT INTO permissions (name, description, category)
SELECT 'CV.DELETE', 'Delete Own CVs', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.DELETE');

INSERT INTO permissions (name, description, category)
SELECT 'CV.PARSE', 'Parse/Upload CV for AI Processing', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.PARSE');

-- API Key Management
INSERT INTO permissions (name, description, category)
SELECT 'API.KEY.CREATE', 'Create API Keys', 'API'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'API.KEY.CREATE');

INSERT INTO permissions (name, description, category)
SELECT 'API.KEY.REVOKE', 'Revoke API Keys', 'API'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'API.KEY.REVOKE');

-- =====================================================
-- 2. Initialize Groups
-- =====================================================

-- Default Group (Free User) - assigned to new registrations
INSERT INTO groups (name, is_system_group, is_default)
SELECT 'Free User', false, true
WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = 'Free User');

-- Super Admin Group - all permissions
INSERT INTO groups (name, is_system_group, is_default)
SELECT 'Super Admin', true, false
WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = 'Super Admin');

-- =====================================================
-- 3. Assign Permissions to Groups
-- =====================================================

-- Free User: Basic CV permissions (CREATE, EXPORT.PDF, DELETE, PARSE)
-- Plus subscription and notification viewing
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Free User' AND p.name = 'CV.CREATE'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Free User' AND p.name = 'CV.EXPORT.PDF'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Free User' AND p.name = 'CV.DELETE'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Free User' AND p.name = 'CV.PARSE'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Free User' AND p.name = 'SUBSCRIPTION.VIEW'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Free User' AND p.name = 'NOTIF.RECEIVE'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

-- Super Admin: ALL permissions
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id FROM groups g, permissions p
WHERE g.name = 'Super Admin'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

-- =====================================================
-- 4. Link Free Plan to Default Group
-- =====================================================

UPDATE subscription_plans 
SET linked_group_id = (SELECT id FROM groups WHERE name = 'Free User')
WHERE name = 'free' 
AND linked_group_id IS NULL;
