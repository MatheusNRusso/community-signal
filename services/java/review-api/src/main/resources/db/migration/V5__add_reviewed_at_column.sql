-- V6: Add reviewed_at column to drafts table
-- Required for tracking when a draft was reviewed (separate from approved_at)
ALTER TABLE drafts
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;

-- Index for queries filtering by review date
CREATE INDEX IF NOT EXISTS idx_drafts_reviewed_at ON drafts (reviewed_at DESC);
