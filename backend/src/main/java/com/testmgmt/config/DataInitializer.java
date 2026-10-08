package com.testmgmt.config;

import com.testmgmt.entity.Project;
import com.testmgmt.entity.Team;
import com.testmgmt.entity.Tenant;
import com.testmgmt.entity.User;
import com.testmgmt.enums.UserRole;
import com.testmgmt.repository.ProjectRepository;
import com.testmgmt.repository.TeamRepository;
import com.testmgmt.repository.TenantRepository;
import com.testmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DataInitializer implements CommandLineRunner {

    /** Fixed UUID of the "DEFAULT" tenant created by V3__add_multi_tenancy.sql. */
    private static final UUID DEFAULT_TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.default-admin.enabled:false}")
    private boolean adminEnabled;

    @Value("${app.default-admin.email:}")
    private String adminEmail;

    @Value("${app.default-admin.username:admin}")
    private String adminUsername;

    @Value("${app.default-admin.password:}")
    private String adminPassword;

    @Value("${app.default-admin.fullname:Platform Admin}")
    private String adminFullName;

    @Value("${app.default-admin.team:Platform}")
    private String adminTeam;

    @Override
    public void run(String... args) {
        backfillMissingTenantIds();
        seedDefaultAdmin();
        seedTeamsUsersAndProjects();
    }

    /**
     * Guardrail for multi-tenant queries.
     *
     * Every service filters by {@code TenantContext.getCurrentTenant()}, which
     * comes from the JWT {@code tenantId} claim, which is copied from
     * {@code users.tenant_id} at login. If any user row has
     * {@code tenant_id = NULL} (created before V3 ran, or via a code path that
     * forgot to set it), that user's JWT will carry no tenant and every list
     * endpoint will silently return an empty result — the classic
     * "database has data but the UI shows nothing" symptom.
     *
     * On startup we back-fill any such rows to the DEFAULT tenant, which is
     * the same value V3 assigned to all pre-existing rows. This is idempotent
     * and safe to run on every boot.
     */
    private void backfillMissingTenantIds() {
        try {
            Integer users = jdbcTemplate.update(
                    "UPDATE users SET tenant_id = ? WHERE tenant_id IS NULL",
                    DEFAULT_TENANT_ID);
            if (users != null && users > 0) {
                log.warn("🔧  Back-filled tenant_id on {} user row(s) to DEFAULT tenant. "
                        + "Affected users must log out and log back in to receive a JWT with the tenant claim.",
                        users);
            }
        } catch (Exception ex) {
            // Table may not exist yet (e.g. very first boot before migrations). Not fatal.
            log.debug("Skipped tenant back-fill: {}", ex.getMessage());
        }
    }

    /**
     * Create default admin only when explicitly enabled via environment config.
     * No hardcoded passwords — credentials come from environment variables only.
     */
    private void seedDefaultAdmin() {
        if (!adminEnabled) {
            log.info("Default admin creation is disabled. Set DEFAULT_ADMIN_ENABLED=true to create.");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠️  DEFAULT_ADMIN_ENABLED is true but email/password not configured. Skipping admin creation.");
            return;
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .fullName(adminFullName)
                    .role(UserRole.ADMIN)
                    .team(adminTeam)
                    .tenantId(DEFAULT_TENANT_ID)   // ← critical: without this the admin sees no data
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("✅  Default admin created: {} (tenant: DEFAULT)", adminEmail);
        }
    }

    /**
     * Seed 3 Teams, members with roles (ADMIN, MANAGER, SME, TESTER) and Projects.
     */
    private void seedTeamsUsersAndProjects() {
        try {
            // 1. Ensure DEFAULT Tenant exists
            Tenant tenant = tenantRepository.findByCode("DEFAULT").orElseGet(() -> {
                log.info("Creating DEFAULT tenant...");
                return tenantRepository.save(Tenant.builder()
                        .id(DEFAULT_TENANT_ID)
                        .code("DEFAULT")
                        .name("Default Organization")
                        .description("Default organization for testing and demo")
                        .active(true)
                        .build());
            });

            // 2. Create 3 Teams
            Team teamCore = teamRepository.findByTenantIdAndCode(tenant.getId(), "QA_CORE")
                    .orElseGet(() -> {
                        log.info("Creating team: QA_CORE");
                        return teamRepository.save(Team.builder()
                                .tenant(tenant)
                                .code("QA_CORE")
                                .name("Core Banking QA Team")
                                .description("Specialized QA team for savings, current accounts, deposits, and general ledger engines.")
                                .teamType("QA")
                                .department("Core Banking")
                                .channel("Branch & Retail")
                                .active(true)
                                .sortOrder(1)
                                .build());
                    });

            Team teamDigital = teamRepository.findByTenantIdAndCode(tenant.getId(), "QA_DIGITAL")
                    .orElseGet(() -> {
                        log.info("Creating team: QA_DIGITAL");
                        return teamRepository.save(Team.builder()
                                .tenant(tenant)
                                .code("QA_DIGITAL")
                                .name("Digital Payments QA Team")
                                .description("QA team for consumer mobile banking apps, UPI, net banking, and merchant settlement.")
                                .teamType("QA")
                                .department("Digital Channels")
                                .channel("Mobile & Web")
                                .active(true)
                                .sortOrder(2)
                                .build());
                    });

            Team teamIntegration = teamRepository.findByTenantIdAndCode(tenant.getId(), "QA_INTEGRATION")
                    .orElseGet(() -> {
                        log.info("Creating team: QA_INTEGRATION");
                        return teamRepository.save(Team.builder()
                                .tenant(tenant)
                                .code("QA_INTEGRATION")
                                .name("Enterprise Integration QA Team")
                                .description("Responsible for middleware orchestration, ISO 20022 SWIFT messaging, and Open Banking APIs.")
                                .teamType("QA")
                                .department("Integration & Middleware")
                                .channel("APIs & Webhooks")
                                .active(true)
                                .sortOrder(3)
                                .build());
                    });

            // 3. Add Members with roles: ADMIN, MANAGER, SME, TESTER
            String defaultPasswordHash = passwordEncoder.encode("Password@123");

            // ADMIN
            User adminUser = userRepository.findByEmail("admin@testgenii.com").orElseGet(() -> {
                log.info("Creating user: admin@testgenii.com (Role: ADMIN)");
                return userRepository.save(User.builder()
                        .email("admin@testgenii.com")
                        .username("admin_platform")
                        .fullName("Platform Administrator")
                        .passwordHash(defaultPasswordHash)
                        .role(UserRole.ADMIN)
                        .team(teamCore.getName())
                        .teamId(teamCore.getId())
                        .tenantId(tenant.getId())
                        .active(true)
                        .build());
            });

            // MANAGER
            User managerUser = userRepository.findByEmail("manager@testgenii.com").orElseGet(() -> {
                log.info("Creating user: manager@testgenii.com (Role: MANAGER)");
                return userRepository.save(User.builder()
                        .email("manager@testgenii.com")
                        .username("qa_manager")
                        .fullName("QA Delivery Manager")
                        .passwordHash(defaultPasswordHash)
                        .role(UserRole.MANAGER)
                        .team(teamCore.getName())
                        .teamId(teamCore.getId())
                        .tenantId(tenant.getId())
                        .active(true)
                        .build());
            });

            // Assign Manager as Lead for Team Core
            if (teamCore.getLeadId() == null) {
                teamCore.setLeadId(managerUser.getId());
                teamRepository.save(teamCore);
            }

            // SME
            User smeUser = userRepository.findByEmail("sme@testgenii.com").orElseGet(() -> {
                log.info("Creating user: sme@testgenii.com (Role: SME)");
                return userRepository.save(User.builder()
                        .email("sme@testgenii.com")
                        .username("banking_sme")
                        .fullName("Senior Banking SME")
                        .passwordHash(defaultPasswordHash)
                        .role(UserRole.SME)
                        .team(teamDigital.getName())
                        .teamId(teamDigital.getId())
                        .tenantId(tenant.getId())
                        .active(true)
                        .build());
            });

            // TESTER
            User testerUser = userRepository.findByEmail("tester@testgenii.com").orElseGet(() -> {
                log.info("Creating user: tester@testgenii.com (Role: TESTER)");
                return userRepository.save(User.builder()
                        .email("tester@testgenii.com")
                        .username("qa_tester")
                        .fullName("Senior Test Engineer")
                        .passwordHash(defaultPasswordHash)
                        .role(UserRole.TESTER)
                        .team(teamIntegration.getName())
                        .teamId(teamIntegration.getId())
                        .tenantId(tenant.getId())
                        .active(true)
                        .build());
            });

            // Additional testers in teams
            if (!userRepository.existsByEmail("tester.core@testgenii.com")) {
                userRepository.save(User.builder()
                        .email("tester.core@testgenii.com")
                        .username("tester_core")
                        .fullName("Core Banking Tester")
                        .passwordHash(defaultPasswordHash)
                        .role(UserRole.TESTER)
                        .team(teamCore.getName())
                        .teamId(teamCore.getId())
                        .tenantId(tenant.getId())
                        .active(true)
                        .build());
            }

            if (!userRepository.existsByEmail("tester.digital@testgenii.com")) {
                userRepository.save(User.builder()
                        .email("tester.digital@testgenii.com")
                        .username("tester_digital")
                        .fullName("Digital Channels Tester")
                        .passwordHash(defaultPasswordHash)
                        .role(UserRole.TESTER)
                        .team(teamDigital.getName())
                        .teamId(teamDigital.getId())
                        .tenantId(tenant.getId())
                        .active(true)
                        .build());
            }

            // 4. Create Projects
            seedProjectIfNotExists("Core Banking Modernization",
                    "End-to-end testing of next-generation core ledger, savings accounts, and term deposits.",
                    managerUser, tenant.getId());

            seedProjectIfNotExists("Mobile Banking & UPI 2.0",
                    "Testing iOS and Android customer banking apps, biometric login, and real-time UPI switch.",
                    managerUser, tenant.getId());

            seedProjectIfNotExists("Open Banking & SWIFT Gateway",
                    "Cross-border ISO 20022 wire messaging, webhook compliance, and developer API sandbox.",
                    managerUser, tenant.getId());

            log.info("✅ Successfully seeded 3 teams, members with roles (ADMIN, MANAGER, SME, TESTER) and projects.");
        } catch (Exception ex) {
            log.warn("⚠️ Could not complete seedTeamsUsersAndProjects: {}", ex.getMessage());
        }
    }

    private void seedProjectIfNotExists(String name, String description, User owner, UUID tenantId) {
        boolean exists = projectRepository.findByActiveTrue().stream()
                .anyMatch(p -> name.equalsIgnoreCase(p.getName()));
        if (!exists) {
            projectRepository.save(Project.builder()
                    .name(name)
                    .description(description)
                    .owner(owner)
                    .tenantId(tenantId)
                    .active(true)
                    .build());
            log.info("Project created: {}", name);
        }
    }
}


