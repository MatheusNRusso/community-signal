-- V6: Seed sample drafts for end-to-end demo (idempotent via fixed UUIDs)
INSERT INTO drafts
    (id, window_id, cluster_id, channel, prompt_version, content, content_hash, source_event_ids, status, created_at, updated_at)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'window-2026-08-10', 'cluster-hn-ai-review',
 'linkedin', '1.2.0',
 'The community is split on AI code-review tools. After analyzing 47 Hacker News comments, one pattern stands out: teams that use AI as a *first pass* (not a final gate) ship faster without losing quality. AI catches the boring bugs; humans catch the dangerous ones. #Engineering #AI',
 'sha256-seed-001', '{}', 'PENDING',
 now() - interval '3 hours', now()),

('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'window-2026-08-10', 'cluster-pg18',
 'twitter', '1.2.0',
 'PostgreSQL 18 is out and the async I/O work is a game changer. Early community benchmarks show 2-3x throughput on analytical workloads. If you are on 15+, this is the upgrade to plan for.',
 'sha256-seed-002', '{}', 'PENDING',
 now() - interval '2 hours', now()),

('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'window-2026-08-09', 'cluster-event-driven',
 'newsletter', '1.2.0',
 'This week in engineering: (1) Event-driven architectures dominate — 3 of the top 10 HN posts touched on Kafka alternatives. (2) The "boring tech" movement gains traction as teams report lower on-call fatigue. (3) Rust in production: loved, but the hiring pipeline is the real bottleneck.',
 'sha256-seed-003', '{}', 'IN_REVIEW',
 now() - interval '26 hours', now()),

('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'window-2026-08-08', 'cluster-kafka',
 'linkedin', '1.2.0',
 'Hot take from this week''s signal digest: most teams don''t need Kafka — they need a queue and discipline. Most upvoted comment? "We replaced Kafka with Postgres LISTEN/NOTIFY and our on-call got quiet." Sometimes the best architecture is the one you can debug at 3am.',
 'sha256-seed-004', '{}', 'APPROVED',
 now() - interval '3 days', now()),

('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'window-2026-08-08', 'cluster-ai-hype',
 'twitter', '1.2.0',
 'AI will replace all backend engineers by 2027. The community agrees that coding is done.',
 'sha256-seed-005', '{}', 'REJECTED',
 now() - interval '3 days', now())

ON CONFLICT (id) DO NOTHING;

UPDATE drafts
SET review_notes = 'Unsubstantiated hype claim, no sources. Does not meet editorial standards.',
    reviewed_at = now() - interval '2 days'
WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05';
