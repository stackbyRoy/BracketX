-- ==========================================================
-- BRACKETX MIGRATION: 20260918_add_rules_column.sql
-- Add custom rules column to tournaments
-- ==========================================================

ALTER TABLE public.tournaments
ADD COLUMN IF NOT EXISTS rules JSONB DEFAULT '[]'::jsonb;
