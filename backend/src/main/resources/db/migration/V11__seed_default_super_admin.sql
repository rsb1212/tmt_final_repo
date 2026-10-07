-- V11 — Seed / ensure the default Super Admin (chenges.md § Plan A).
--
-- Target: rahul.bhagat@bajajlife.com — must have organization-wide access.
--
-- Idempotent:
--   * If the user already exists → promote to role=ADMIN, is_super_admin=TRUE, active=TRUE.
--   * If the user does NOT exist → insert a new row.
--
-- Notes:
--   * password_hash is a non-functional BCrypt placeholder. Login is via IDEM
--     SSO; this column only satisfies the NOT NULL constraint and will NEVER
--     authenticate anyone (BCrypt string generated for an unknown random pwd).
--   * tenant_id / team_id / team are left NULL because super admins bypass
--     team isolation by design (see TeamAccessGuard).

INSERT INTO users (
    id,
    username,
    email,
    password_hash,
    full_name,
    role,
    active,
    is_super_admin,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid(),
    'rahul.bhagat@bajajlife.com',
    'rahul.bhagat@bajajlife.com',
    '$2a$10$CwTycUXWue0Thq9StjUM0uJ8E6Vv9JgqkQp6l9Jw8l7qkDqNqj5F6',  -- disabled; SSO only
    'Rahul Bhagat',
    'ADMIN',
    TRUE,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO UPDATE
SET role           = 'ADMIN',
    is_super_admin = TRUE,
    active         = TRUE,
    updated_at     = NOW();
