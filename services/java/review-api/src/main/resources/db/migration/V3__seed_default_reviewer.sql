INSERT INTO review_users (username, password, role, active)
SELECT 'reviewer-1',
       '$2b$10$FT6b9ODL3XYAuA3VLajTjOroNoNgGJz.oAgjd2CcfoEs7/qW7nBUy',
       'ROLE_REVIEWER',
       true
WHERE NOT EXISTS (
    SELECT 1 FROM review_users WHERE username = 'reviewer-1'
);
