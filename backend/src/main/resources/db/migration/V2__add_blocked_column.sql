-- Flyway Migration: Add blocked status to users table
ALTER TABLE users ADD COLUMN is_blocked BOOLEAN DEFAULT FALSE;
