-- V3: (deprecated) Hardcoded reviewer seed removed.
-- Admin users are bootstrapped at runtime by DataInitializer
-- (ADMIN_EMAIL / ADMIN_PASSWORD). Reviewers come from GitHub OAuth.
-- Kept as no-op to preserve Flyway ordering and document the decision.
SELECT 1;
