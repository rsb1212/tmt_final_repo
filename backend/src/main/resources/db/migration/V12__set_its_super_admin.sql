-- V12 — Make rahul.bhagat@its.bajajlife.com the ONLY Super Admin.
--
-- SSO logs this user in as 'rahul.bhagat@its.bajajlife.com' (see server log),
-- which differs from the 'rahul.bhagat@bajajlife.com' row seeded in V11.
--
-- Only this user may:
--   * access the Tenants page / tenant APIs
--   * create / edit / delete Teams
--   * add / remove Team members
-- (enforced in the backend via @PreAuthorize("@superAdminGuard.check()")).
--
-- Idempotent.

-- 1) Revoke the Super Admin flag from everyone else.
--    Their role (ADMIN/MANAGER/...) is NOT changed; they just lose org-wide rights.
UPDATE users
   SET is_super_admin = FALSE,
       updated_at     = NOW()
 WHERE is_super_admin = TRUE
   AND LOWER(email) <> 'rahul.bhagat@its.bajajlife.com';

-- 2) Ensure the target user exists and is an active Super Admin.
INSERT INTO users (
    id,
    username,
    email,
    password_hash,
    full_name,
    role,
    active,
    is_super_admin,
    tenant_id,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid(),
    'rahul.bhagat@its.bajajlife.com',
    'rahul.bhagat@its.bajajlife.com',
    '$2a$10$CwTycUXWue0Thq9StjUM0uJ8E6Vv9JgqkQp6l9Jw8l7qkDqNqj5F6',  -- disabled; SSO only
    'Rahul Bhagat',
    'ADMIN',
    TRUE,
    TRUE,
    '00000000-0000-0000-0000-000000000001',
    NOW(),
    NOW()
)
ON CONFLICT (username) DO UPDATE
SET role           = 'ADMIN',
    is_super_admin = TRUE,
    active         = TRUE,
    updated_at     = NOW();

-- Safety net in case the existing row's username differs from its email.
UPDATE users
   SET role           = 'ADMIN',
       is_super_admin = TRUE,
       active         = TRUE,
       updated_at     = NOW()
 WHERE LOWER(email) = 'rahul.bhagat@its.bajajlife.com';
