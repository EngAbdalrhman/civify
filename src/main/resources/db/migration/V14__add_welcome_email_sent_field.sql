-- Add welcome_email_sent column to track whether a user has received their welcome email
ALTER TABLE users ADD COLUMN welcome_email_sent BOOLEAN NOT NULL DEFAULT false;
