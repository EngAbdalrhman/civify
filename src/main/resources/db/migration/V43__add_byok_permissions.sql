-- BYOK permissions (not auto-granted to any group — assign via admin UI when ready)

INSERT INTO permissions (name, description, category)
SELECT 'AI.BYOK_USE', 'Store and use personal AI provider API keys', 'AI'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'AI.BYOK_USE');

INSERT INTO permissions (name, description, category)
SELECT 'AI.BYOK_STRICT_MODE', 'Enable strict BYOK-only mode without platform fallback', 'AI'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'AI.BYOK_STRICT_MODE');

INSERT INTO permissions (name, description, category)
SELECT 'AI.BYOK_MANAGE_ORG', 'Manage organization AI provider keys', 'AI'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'AI.BYOK_MANAGE_ORG');
