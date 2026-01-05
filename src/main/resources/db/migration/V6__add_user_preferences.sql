-- Add user preference columns for language and theme
-- These are used to personalize email templates
ALTER TABLE users ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'EN';
ALTER TABLE users ADD COLUMN preferred_theme VARCHAR(10) NOT NULL DEFAULT 'LIGHT';
