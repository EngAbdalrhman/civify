INSERT INTO permissions (name, description, category)
SELECT 'CREATE.BLOG', 'Create a Blog', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CREATE.BLOG');


INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
  AND p.name = 'CREATE.BLOG'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);