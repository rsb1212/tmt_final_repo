-- V9__add_team_id_to_projects.sql
-- Team-based project isolation (chenges.md § 5 / § 7)
-- Adds a nullable team_id foreign key so each project can be linked to a Team.
-- Nullable = shared/legacy project (visible to everyone) — preserves backward compatibility.

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS team_id UUID;

-- Match Team entity's FK expectation
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.table_constraints
        WHERE  constraint_name = 'fk_projects_team'
    ) THEN
        ALTER TABLE projects
            ADD CONSTRAINT fk_projects_team
            FOREIGN KEY (team_id) REFERENCES teams(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- Index used by team-scoped project lookups (matches @Index in Project entity)
CREATE INDEX IF NOT EXISTS idx_projects_team ON projects(team_id);

COMMENT ON COLUMN projects.team_id IS
    'Team that owns this project. NULL = shared/unassigned (visible to all).';
