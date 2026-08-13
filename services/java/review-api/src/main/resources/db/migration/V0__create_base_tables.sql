-- V0: Create base tables (runs before V1-V3)
-- This mirrors infra/postgres/migrations.sql for production deployment

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;

-- Community events
CREATE TABLE IF NOT EXISTS community_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type     TEXT        NOT NULL,
    source_id       TEXT        NOT NULL,
    content_hash    TEXT        NOT NULL,
    content_text    TEXT,
    author_id       TEXT,
    community_id    TEXT,
    language        VARCHAR(5),
    engagement_meta JSONB       NOT NULL DEFAULT '{}',
    raw_payload     JSONB,
    enriched_at     TIMESTAMPTZ NOT NULL,
    ingested_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_events_content_hash  ON community_events (content_hash);
CREATE INDEX IF NOT EXISTS idx_events_ingested_at   ON community_events (ingested_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_community     ON community_events (community_id, ingested_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_source        ON community_events (source_type, ingested_at DESC);

-- Embedding store
CREATE TABLE IF NOT EXISTS event_embeddings (
    content_hash    TEXT        PRIMARY KEY,
    embedding       VECTOR(384) NOT NULL,
    model_version   TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_embeddings_ivfflat
    ON event_embeddings USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Engagement scores
CREATE TABLE IF NOT EXISTS engagement_scores (
    content_hash    TEXT        PRIMARY KEY,
    score           FLOAT       NOT NULL,
    reactions       INT         NOT NULL DEFAULT 0,
    reply_count     INT         NOT NULL DEFAULT 0,
    share_count     INT         NOT NULL DEFAULT 0,
    scored_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scores_scored_at ON engagement_scores (scored_at DESC);

-- Prompt templates
CREATE TABLE IF NOT EXISTS prompt_templates (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel         TEXT        NOT NULL CHECK (channel IN ('newsletter', 'linkedin', 'twitter')),
    version         TEXT        NOT NULL,
    template_text   TEXT        NOT NULL,
    eval_score      FLOAT,
    is_active       BOOLEAN     NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (channel, version)
);

CREATE INDEX IF NOT EXISTS idx_prompts_active ON prompt_templates (channel, is_active);

-- Drafts (base structure)
CREATE TYPE draft_status AS ENUM (
    'PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'REVISED', 'PUBLISHED'
);

CREATE TABLE IF NOT EXISTS drafts (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    window_id       TEXT         NOT NULL,
    cluster_id      TEXT         NOT NULL,
    channel         TEXT         NOT NULL CHECK (channel IN ('newsletter', 'linkedin', 'twitter')),
    prompt_version  TEXT         NOT NULL,
    content         TEXT         NOT NULL,
    content_hash    TEXT         NOT NULL,
    source_event_ids UUID[]      NOT NULL DEFAULT '{}',
    status          draft_status NOT NULL DEFAULT 'PENDING',
    reviewed_by     UUID,
    review_notes    TEXT,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_drafts_status_channel ON drafts (status, channel);
CREATE INDEX IF NOT EXISTS idx_drafts_window         ON drafts (window_id);
CREATE INDEX IF NOT EXISTS idx_drafts_created_at     ON drafts (created_at DESC);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER drafts_updated_at
    BEFORE UPDATE ON drafts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Draft versions
CREATE TABLE IF NOT EXISTS draft_versions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id        UUID        NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    version_number  INT         NOT NULL,
    content         TEXT        NOT NULL,
    changed_by      TEXT        NOT NULL,
    change_reason   TEXT,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (draft_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_draft_versions_draft ON draft_versions (draft_id, version_number);

-- Published drafts
CREATE TABLE IF NOT EXISTS published_drafts (
    draft_id        UUID        NOT NULL REFERENCES drafts(id),
    channel         TEXT        NOT NULL,
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    external_id     TEXT,
    metadata        JSONB       NOT NULL DEFAULT '{}',
    PRIMARY KEY (draft_id, channel)
);

-- Audit log
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL   PRIMARY KEY,
    draft_id        UUID        NOT NULL,
    channel         TEXT        NOT NULL,
    action          TEXT        NOT NULL,
    actor           TEXT,
    metadata        JSONB       NOT NULL DEFAULT '{}',
    logged_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_draft   ON audit_log (draft_id, logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logged  ON audit_log (logged_at DESC);

-- Feedback events
CREATE TABLE IF NOT EXISTS feedback_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id        UUID        NOT NULL REFERENCES drafts(id),
    editor_id       TEXT        NOT NULL,
    action          TEXT        NOT NULL,
    edit_distance   INT,
    rejection_reason TEXT,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feedback_draft    ON feedback_events (draft_id);
CREATE INDEX IF NOT EXISTS idx_feedback_recorded ON feedback_events (recorded_at DESC);
