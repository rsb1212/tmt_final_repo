-- Seed 3 Teams, Members with roles (ADMIN, MANAGER, SME, TESTER) and Projects
-- Safe to execute multiple times (idempotent)

-- 1. Ensure DEFAULT Tenant exists
INSERT INTO tenants (id, code, name, description, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'DEFAULT',
    'Default Organization',
    'Default multi-tenant organization',
    true,
    NOW(),
    NOW()
) ON CONFLICT (code) DO NOTHING;

-- 2. Create 3 Teams
-- Team 1: Core Banking QA Team
INSERT INTO teams (id, tenant_id, code, name, description, team_type, department, channel, active, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111101',
    '00000000-0000-0000-0000-000000000001',
    'QA_CORE',
    'Core Banking QA Team',
    'Specialized QA team for savings, current accounts, deposits, and general ledger engines.',
    'QA',
    'Core Banking',
    'Branch & Retail',
    true,
    NOW(),
    NOW()
) ON CONFLICT (code, tenant_id) DO NOTHING;

-- Team 2: Digital Channels & Payments QA Team
INSERT INTO teams (id, tenant_id, code, name, description, team_type, department, channel, active, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111102',
    '00000000-0000-0000-0000-000000000001',
    'QA_DIGITAL',
    'Digital Payments QA Team',
    'QA team for consumer mobile banking apps, UPI, net banking, and merchant settlement.',
    'QA',
    'Digital Channels',
    'Mobile & Web',
    true,
    NOW(),
    NOW()
) ON CONFLICT (code, tenant_id) DO NOTHING;

-- Team 3: Enterprise Integration & API Team
INSERT INTO teams (id, tenant_id, code, name, description, team_type, department, channel, active, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111103',
    '00000000-0000-0000-0000-000000000001',
    'QA_INTEGRATION',
    'Enterprise Integration QA Team',
    'Responsible for middleware orchestration, ISO 20022 SWIFT messaging, and Open Banking APIs.',
    'QA',
    'Integration & Middleware',
    'APIs & Webhooks',
    true,
    NOW(),
    NOW()
) ON CONFLICT (code, tenant_id) DO NOTHING;

-- 3. Add Members with roles: ADMIN, MANAGER, SME, TESTER
-- Password for all seed users is: Password@123 ($2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi is standard BCrypt, or Spring Boot BCrypt hash)
-- Hash below is BCrypt of 'Password@123': $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.

-- ADMIN: System Administrator in Team 1
INSERT INTO users (id, tenant_id, team_id, username, email, password_hash, full_name, role, team, active, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222201',
    '00000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111101',
    'admin_platform',
    'admin@testgenii.com',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.',
    'Platform Administrator',
    'ADMIN',
    'Core Banking QA Team',
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    team_id = EXCLUDED.team_id,
    role = 'ADMIN',
    team = EXCLUDED.team;

-- MANAGER: QA Delivery Manager (Lead of Team 1)
INSERT INTO users (id, tenant_id, team_id, username, email, password_hash, full_name, role, team, active, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222202',
    '00000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111101',
    'qa_manager',
    'manager@testgenii.com',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.',
    'QA Delivery Manager',
    'MANAGER',
    'Core Banking QA Team',
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    team_id = EXCLUDED.team_id,
    role = 'MANAGER',
    team = EXCLUDED.team;

-- Set lead_id on Team 1 to the Manager
UPDATE teams SET lead_id = '22222222-2222-2222-2222-222222222202'
WHERE id = '11111111-1111-1111-1111-111111111101';

-- SME: Subject Matter Expert in Team 2
INSERT INTO users (id, tenant_id, team_id, username, email, password_hash, full_name, role, team, active, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222203',
    '00000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111102',
    'banking_sme',
    'sme@testgenii.com',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.',
    'Senior Banking SME',
    'SME',
    'Digital Payments QA Team',
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    team_id = EXCLUDED.team_id,
    role = 'SME',
    team = EXCLUDED.team;

-- TESTER: Senior Test Engineer in Team 3
INSERT INTO users (id, tenant_id, team_id, username, email, password_hash, full_name, role, team, active, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222204',
    '00000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111103',
    'qa_tester',
    'tester@testgenii.com',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.',
    'Senior Test Engineer',
    'TESTER',
    'Enterprise Integration QA Team',
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    team_id = EXCLUDED.team_id,
    role = 'TESTER',
    team = EXCLUDED.team;

-- Additional dedicated testers per team
INSERT INTO users (id, tenant_id, team_id, username, email, password_hash, full_name, role, team, active, created_at, updated_at)
VALUES 
(
    '22222222-2222-2222-2222-222222222205',
    '00000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111101',
    'tester_core',
    'tester.core@testgenii.com',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.',
    'Core Banking Tester',
    'TESTER',
    'Core Banking QA Team',
    true,
    NOW(),
    NOW()
),
(
    '22222222-2222-2222-2222-222222222206',
    '00000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111102',
    'tester_digital',
    'tester.digital@testgenii.com',
    '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.',
    'Digital Channels Tester',
    'TESTER',
    'Digital Payments QA Team',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- 4. Create Projects
-- Project 1: Core Banking Modernization
INSERT INTO projects (id, tenant_id, name, description, owner_id, active, created_at, updated_at)
VALUES (
    '33333333-3333-3333-3333-333333333301',
    '00000000-0000-0000-0000-000000000001',
    'Core Banking Modernization',
    'End-to-end testing of next-generation core ledger, savings accounts, and term deposits.',
    '22222222-2222-2222-2222-222222222202',
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- Project 2: Mobile Banking & UPI 2.0
INSERT INTO projects (id, tenant_id, name, description, owner_id, active, created_at, updated_at)
VALUES (
    '33333333-3333-3333-3333-333333333302',
    '00000000-0000-0000-0000-000000000001',
    'Mobile Banking & UPI 2.0',
    'Testing iOS and Android customer banking apps, biometric login, and real-time UPI switch.',
    '22222222-2222-2222-2222-222222222202',
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;

-- Project 3: Open Banking & SWIFT Gateway
INSERT INTO projects (id, tenant_id, name, description, owner_id, active, created_at, updated_at)
VALUES (
    '33333333-3333-3333-3333-333333333303',
    '00000000-0000-0000-0000-000000000001',
    'Open Banking & SWIFT Gateway',
    'Cross-border ISO 20022 wire messaging, webhook compliance, and developer API sandbox.',
    '22222222-2222-2222-2222-222222222202',
    true,
    NOW(),
    NOW()
) ON CONFLICT DO NOTHING;
