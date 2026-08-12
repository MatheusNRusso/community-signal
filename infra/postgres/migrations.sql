-- ─────────────────────────────────────────────────────────────
--  02_migrations.sql — Schema definition (V1)
--  Managed by Flyway in production. Run manually in local dev.
-- ─────────────────────────────────────────────────────────────

-- ── Community events (canonical, post-enrichment) ─────────────

CREATE TABLE IF NOT EXISTS community_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type     TEXT        NOT NULL,               -- 'discord' | 'slack' | 'reddit' | 'rss'
    source_id       TEXT        NOT NULL,               -- platform-native post/message ID
    content_hash    TEXT        NOT NULL,               -- SHA-256 of normalized content
    content_text    TEXT,
    author_id       TEXT,
    community_id    TEXT,
    language        VARCHAR(5),                         -- ISO 639-1 language code
    engagement_meta JSONB       NOT NULL DEFAULT '{}',  -- {reactions, replies, shares, views}
    raw_payload     JSONB,                              -- original source payload (kept for reprocessing)
    enriched_at     TIMESTAMPTZ NOT NULL,
    ingested_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_events_content_hash  ON community_events (content_hash);
CREATE INDEX IF NOT EXISTS idx_events_ingested_at   ON community_events (ingested_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_community     ON community_events (community_id, ingested_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_source        ON community_events (source_type, ingested_at DESC);

-- ── Embedding store (384-dim MiniLM-L6-v2) ───────────────────

CREATE TABLE IF NOT EXISTS event_embeddings (
    content_hash    TEXT        PRIMARY KEY,
    embedding       VECTOR(384) NOT NULL,
    model_version   TEXT        NOT NULL,               -- e.g. 'minilm-l6-v2-1.0'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- IVFFlat index: good for up to ~5M rows; revisit beyond that threshold
CREATE INDEX IF NOT EXISTS idx_embeddings_ivfflat
    ON event_embeddings USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- ── Engagement scores (denormalized cache for ranking) ────────

CREATE TABLE IF NOT EXISTS engagement_scores (
    content_hash    TEXT        PRIMARY KEY,
    score           FLOAT       NOT NULL,
    reactions       INT         NOT NULL DEFAULT 0,
    reply_count     INT         NOT NULL DEFAULT 0,
    share_count     INT         NOT NULL DEFAULT 0,
    scored_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL               -- TTL gate for stale score cleanup
);

CREATE INDEX IF NOT EXISTS idx_scores_scored_at ON engagement_scores (scored_at DESC);

-- ── Prompt templates (versioned) ─────────────────────────────

CREATE TABLE IF NOT EXISTS prompt_templates (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel         TEXT        NOT NULL CHECK (channel IN ('newsletter', 'linkedin', 'twitter')),
    version         TEXT        NOT NULL,               -- semver e.g. '1.0.0'
    template_text   TEXT        NOT NULL,
    eval_score      FLOAT,                             -- automated quality score (0-1)
    is_active       BOOLEAN     NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (channel, version)
);

CREATE INDEX IF NOT EXISTS idx_prompts_active ON prompt_templates (channel, is_active);

-- ── Drafts ────────────────────────────────────────────────────

CREATE TYPE draft_status AS ENUM (
    'PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'REVISED', 'PUBLISHED'
);

CREATE TABLE IF NOT EXISTS drafts (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    window_id       TEXT         NOT NULL,              -- ranking window that produced this draft
    cluster_id      TEXT         NOT NULL,              -- HDBSCAN cluster identifier
    channel         TEXT         NOT NULL CHECK (channel IN ('newsletter', 'linkedin', 'twitter')),
    prompt_version  TEXT         NOT NULL,
    content         TEXT         NOT NULL,
    content_hash    TEXT         NOT NULL,              -- SHA-256(content) for LLM output cache dedup
    source_event_ids UUID[]      NOT NULL DEFAULT '{}', -- references to community_events.id
    status          draft_status NOT NULL DEFAULT 'PENDING',
    reviewed_by     UUID,                              -- editor user ID
    review_notes    TEXT,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_drafts_status_channel ON drafts (status, channel);
CREATE INDEX IF NOT EXISTS idx_drafts_window         ON drafts (window_id);
CREATE INDEX IF NOT EXISTS idx_drafts_created_at     ON drafts (created_at DESC);

-- Auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER drafts_updated_at
    BEFORE UPDATE ON drafts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Draft version history (immutable append-only) ─────────────

CREATE TABLE IF NOT EXISTS draft_versions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id        UUID        NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    version_number  INT         NOT NULL,
    content         TEXT        NOT NULL,
    changed_by      TEXT        NOT NULL,              -- 'llm' | editor UUID string
    change_reason   TEXT,                              -- 'initial_generation' | 'editor_edit' | 'llm_regen'
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (draft_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_draft_versions_draft ON draft_versions (draft_id, version_number);

-- ── Published drafts (idempotency guard) ─────────────────────

CREATE TABLE IF NOT EXISTS published_drafts (
    draft_id        UUID        NOT NULL REFERENCES drafts(id),
    channel         TEXT        NOT NULL,
    published_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    external_id     TEXT,                              -- platform post ID (LinkedIn URN, Tweet ID)
    metadata        JSONB       NOT NULL DEFAULT '{}',
    PRIMARY KEY (draft_id, channel)
);

-- ── Audit log (immutable, append-only) ───────────────────────

CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL   PRIMARY KEY,
    draft_id        UUID        NOT NULL,
    channel         TEXT        NOT NULL,
    action          TEXT        NOT NULL,              -- 'publish_attempt' | 'publish_success' | 'publish_failure' | 'dlq'
    actor           TEXT,                              -- service name or editor ID
    metadata        JSONB       NOT NULL DEFAULT '{}',
    logged_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_draft   ON audit_log (draft_id, logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logged  ON audit_log (logged_at DESC);

-- ── Feedback events (editorial signals for offline training) ──

CREATE TABLE IF NOT EXISTS feedback_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id        UUID        NOT NULL REFERENCES drafts(id),
    editor_id       TEXT        NOT NULL,
    action          TEXT        NOT NULL,              -- 'approve' | 'reject' | 'edit'
    edit_distance   INT,                               -- Levenshtein distance when action='edit'
    rejection_reason TEXT,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feedback_draft    ON feedback_events (draft_id);
CREATE INDEX IF NOT EXISTS idx_feedback_recorded ON feedback_events (recorded_at DESC);
