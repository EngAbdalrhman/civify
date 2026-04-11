INSERT INTO permissions (name, description, category)
SELECT 'CV.JOB_MATCH', 'Analyze Resume against Job Description', 'CV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'CV.JOB_MATCH');

-- Add to Super Admin Group
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Super Admin'
  AND p.name = 'CV.JOB_MATCH'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);

-- Add to Free User Group (since it has its own limits)
INSERT INTO group_permissions (group_id, permission_id)
SELECT g.id, p.id
FROM groups g, permissions p
WHERE g.name = 'Free User'
  AND p.name = 'CV.JOB_MATCH'
  AND NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = g.id AND gp.permission_id = p.id
);
