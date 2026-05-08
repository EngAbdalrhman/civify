INSERT INTO permissions (name, description, category)
SELECT 'UPDATE.BLOG', 'Update a Blog', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'UPDATE.BLOG');


INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
  AND p.name = 'UPDATE.BLOG'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);


INSERT INTO permissions (name, description, category)
SELECT 'DELETE.BLOG', 'Delete a Blog', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'DELETE.BLOG');


INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
  AND p.name = 'DELETE.BLOG'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);


INSERT INTO permissions (name, description, category)
SELECT 'VIEW.BLOG', 'View a Blog', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW.BLOG');


INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
  AND p.name = 'VIEW.BLOG'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);