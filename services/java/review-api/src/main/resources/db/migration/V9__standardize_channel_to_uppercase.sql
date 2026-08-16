-- V9: Standardize channel values to UPPERCASE to match Java enum convention
-- Root cause: V6 seeded lowercase values ('twitter', 'linkedin', 'newsletter')
-- but Channel enum uses uppercase (TWITTER, LINKEDIN, NEWSLETTER).

-- Drop the old CHECK constraint (lowercase)
ALTER TABLE drafts DROP CONSTRAINT IF EXISTS drafts_channel_check;

-- Update existing data to UPPERCASE
UPDATE drafts SET channel = UPPER(channel) WHERE channel IS NOT NULL;

-- Add new CHECK constraint (uppercase)
ALTER TABLE drafts ADD CONSTRAINT drafts_channel_check 
    CHECK (channel IN ('NEWSLETTER', 'LINKEDIN', 'TWITTER'));
