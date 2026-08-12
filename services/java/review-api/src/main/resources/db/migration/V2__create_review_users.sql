-- Create table for storing reviewer authentication credentials and roles
CREATE TABLE IF NOT EXISTS review_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_REVIEWER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Add comment to explain the table's purpose
COMMENT ON TABLE review_users IS 'Stores authentication credentials, active status, and role information for platform reviewers';

-- Create index for fast username lookup during login authentication
CREATE INDEX IF NOT EXISTS idx_review_users_username ON review_users (username);
