-- Migration to add has_completed_setup to users table
ALTER TABLE users ADD COLUMN has_completed_setup BOOLEAN DEFAULT FALSE;

-- Existing users should not be forced to complete setup
UPDATE users SET has_completed_setup = TRUE;
