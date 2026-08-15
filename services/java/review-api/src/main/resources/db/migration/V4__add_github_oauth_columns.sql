-- V5: Add GitHub OAuth columns to review_users
ALTER TABLE review_users
    ADD COLUMN IF NOT EXISTS github_id BIGINT,
    ADD COLUMN IF NOT EXISTS github_username VARCHAR(100),
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_github_user BOOLEAN DEFAULT FALSE;

-- Index para lookup rápido por github_username
CREATE INDEX IF NOT EXISTS idx_review_users_github_username 
    ON review_users(github_username) 
    WHERE github_username IS NOT NULL;

-- Index para lookup por github_id
CREATE INDEX IF NOT EXISTS idx_review_users_github_id 
    ON review_users(github_id) 
    WHERE github_id IS NOT NULL;

-- Unique constraint para github_id (cada GitHub user = 1 conta)
ALTER TABLE review_users 
    ADD CONSTRAINT uq_review_users_github_id UNIQUE (github_id);
