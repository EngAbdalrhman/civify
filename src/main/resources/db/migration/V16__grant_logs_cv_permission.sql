-- Ensure LOGS.CV permission exists
INSERT INTO permissions (name, description, category)
SELECT 'LOGS.CV', 'View CV Operation Logs', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'LOGS.CV');

-- Grant LOGS.CV to Super Admin
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
AND p.name = 'LOGS.CV'
AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);
