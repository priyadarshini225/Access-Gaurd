-- V2 Migration: Add expires_at column to application_access table if missing
ALTER TABLE application_access ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
