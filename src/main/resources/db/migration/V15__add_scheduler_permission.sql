-- Add SCHEDULER_TRIGGER permission
INSERT INTO permissions (name, description, category)
SELECT 'SCHEDULER_TRIGGER', 'Trigger Manual Scheduled Jobs', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'SCHEDULER_TRIGGER');

-- Assign to Super Admin
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id 
FROM groups g, permissions p
WHERE g.name = 'Super Admin' 
AND p.name = 'SCHEDULER_TRIGGER'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp 
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);
