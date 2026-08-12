-- ─────────────────────────────────────────────────────────────
--  01_init.sql — Extensions and base configuration
--  Runs once on first container startup, before migrations.sql
-- ─────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;        -- pgvector: ANN similarity search
CREATE EXTENSION IF NOT EXISTS pg_trgm;       -- trigram similarity for near-dup text search
CREATE EXTENSION IF NOT EXISTS btree_gin;     -- GIN indexes on composite types

-- V2: Align drafts with review-api domain model
ALTER TABLE drafts
    ADD COLUMN IF NOT EXISTS guardrail_score   FLOAT,
    ADD COLUMN IF NOT EXISTS guardrail_passed  BOOLEAN,
    ADD COLUMN IF NOT EXISTS guardrail_reasons JSONB,
    ADD COLUMN IF NOT EXISTS llm_model         TEXT,
    ADD COLUMN IF NOT EXISTS reviewer_id       TEXT,
    ADD COLUMN IF NOT EXISTS reviewer_note     TEXT,
    ADD COLUMN IF NOT EXISTS reviewed_at       TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS generated_at      TIMESTAMP WITH TIME ZONE;

ALTER TABLE drafts DROP CONSTRAINT IF EXISTS drafts_channel_check;
ALTER TABLE drafts ADD CONSTRAINT drafts_channel_check
    CHECK (upper(channel) = ANY (ARRAY['NEWSLETTER','LINKEDIN','TWITTER']));

CREATE INDEX IF NOT EXISTS idx_drafts_guardrail_score ON drafts(guardrail_score DESC);
