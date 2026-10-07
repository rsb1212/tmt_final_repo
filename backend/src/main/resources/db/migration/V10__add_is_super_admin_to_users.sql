-- V10 — Introduce explicit "super admin" flag on users (chenges.md § Plan A).
--
-- Rationale
--   The UserRole enum (ADMIN, MANAGER, SME, TESTER, VIEWER) is kept as-is to
--   avoid a breaking change. A new boolean column distinguishes:
--     * SUPER ADMIN  — is_super_admin = TRUE  — bypasses team isolation,
--                       sees ALL teams / projects / data (organization-wide).
--     * TEAM ADMIN   — role = 'ADMIN' AND is_super_admin = FALSE — admin
--                       privileges but scoped to their own team only.
--     * MANAGER / SME / TESTER / VIEWER — team-scoped as before.
--
-- Backward compatibility
--   To avoid locking out existing administrators during rollout, every
--   current user whose role = 'ADMIN' is promoted to is_super_admin = TRUE
--   on migration. The DBA / security team should then manually DEMOTE the
--   team admins back to is_super_admin = FALSE after verifying the list of
--   true organization-wide super-admins.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_super_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: preserve current behaviour for pre-existing ADMIN users.
UPDATE users
   SET is_super_admin = TRUE
 WHERE role = 'ADMIN'
   AND is_super_admin = FALSE;

CREATE INDEX IF NOT EXISTS idx_users_is_super_admin
    ON users (is_super_admin)
    WHERE is_super_admin = TRUE;
