-- V8: Synchronize drafts table with Draft.java entity
-- Adds all missing columns identified in the entity that were not in V0.
-- Each ADD uses IF NOT EXISTS so this migration is idempotent.

-- ── Guardrail fields (AI safety layer) ────────────────────────────────────
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS guardrail_score DOUBLE PRECISION;
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS guardrail_passed BOOLEAN;
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS guardrail_reasons JSONB NOT NULL DEFAULT '[]'::jsonb;

-- ── LLM provenance ────────────────────────────────────────────────────────
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS llm_model VARCHAR(100);

-- ── Reviewer tracking (entity uses String IDs, not UUID FKs) ──────────────
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS reviewer_id VARCHAR(100);
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS reviewer_note TEXT;

-- ── Indexes for new columns ───────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_drafts_guardrail_passed ON drafts (guardrail_passed) WHERE guardrail_passed IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_drafts_llm_model ON drafts (llm_model);
CREATE INDEX IF NOT EXISTS idx_drafts_reviewer_id ON drafts (reviewer_id) WHERE reviewer_id IS NOT NULL;

-- ── Backfill legacy columns so entity fields have reasonable values ───────
UPDATE drafts
SET reviewer_id = reviewed_by::TEXT,
    reviewer_note = COALESCE(review_notes, '')
WHERE reviewer_id IS NULL AND reviewed_by IS NOT NULL;
