package com.testmgmt.service;

import com.testmgmt.entity.Defect;
import com.testmgmt.entity.Project;
import com.testmgmt.entity.TestCase;
import com.testmgmt.entity.TestExecution;
import com.testmgmt.entity.User;
import com.testmgmt.repository.DefectRepository;
import com.testmgmt.repository.ProjectRepository;
import com.testmgmt.repository.TestCaseRepository;
import com.testmgmt.repository.TestExecutionRepository;
import com.testmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Central guard for team-based data isolation (per {@code chenges.md}).
 * <p>
 * Rules:
 * <ul>
 *   <li>Super-Admins ({@code user.isSuperAdmin == true}) bypass all team checks.</li>
 *   <li>Unauthenticated context (jobs, IDEM bootstrap) bypasses — non-web callers use service layer directly.</li>
 *   <li>Users with no {@code teamId} bypass — backward-compatible for existing installations
 *       until they are assigned a team.</li>
 *   <li>Projects with {@code teamId == null} are treated as shared and are visible to everyone.</li>
 *   <li>Otherwise the caller's teamId must equal the project's teamId; else {@link AccessDeniedException}.</li>
 * </ul>
 * <p>
 * The guard resolves TestCase / Defect / TestExecution to their owning Project and applies the
 * same rule, so downstream API access can't be used to bypass team isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamAccessGuard {

    private final UserRepository          userRepository;
    private final ProjectRepository       projectRepository;
    private final TestCaseRepository      testCaseRepository;
    private final DefectRepository        defectRepository;
    private final TestExecutionRepository testExecutionRepository;

    // ── Public: identity of the "restriction" ─────────────────────────────────

    /**
     * Returns the current user's teamId if their view should be restricted to their team's data,
     * else {@link Optional#empty()} meaning "no restriction" (ADMIN, unauthenticated,
     * or user without a team assignment).
     */
    public Optional<UUID> currentUserTeamIdIfRestricted() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return Optional.empty();
            String email = auth.getName();
            if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
                return Optional.empty();
            }
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return Optional.empty();

            // ── chenges.md § Plan A — Super-admins see everything.
            // Note we intentionally key off the explicit flag, NOT off
            // ROLE_ADMIN, so "Team Admins" (role=ADMIN, is_super_admin=false)
            // remain correctly restricted to their own team's data.
            if (Boolean.TRUE.equals(user.getIsSuperAdmin())) return Optional.empty();

            // Users with no team assignment bypass as well, so legacy accounts
            // keep working until an admin assigns them a team. This is the same
            // backward-compatible behaviour the guard had before Plan A.
            return Optional.ofNullable(user.getTeamId());
        } catch (Exception e) {
            // Fail-open — don't break existing flows if security context is not populated
            return Optional.empty();
        }
    }

    // ── Public: visibility predicates ─────────────────────────────────────────

    /** True if the current user may see the given project (or if there is no restriction). */
    public boolean canAccessProject(UUID projectId) {
        Optional<UUID> restrict = currentUserTeamIdIfRestricted();
        if (restrict.isEmpty()) return true;
        Project p = projectRepository.findById(projectId).orElse(null);
        if (p == null) return true;              // let downstream code produce the 404
        if (p.getTeamId() == null) return true;  // shared project
        return restrict.get().equals(p.getTeamId());
    }

    /** True if the given (already-loaded) project is visible to the current user. */
    public boolean canAccessProject(Project project) {
        if (project == null) return true;
        Optional<UUID> restrict = currentUserTeamIdIfRestricted();
        if (restrict.isEmpty()) return true;
        if (project.getTeamId() == null) return true;
        return restrict.get().equals(project.getTeamId());
    }

    // ── Public: assertions (throw 403 if not allowed) ─────────────────────────

    public void assertProjectAccess(UUID projectId) {
        if (projectId == null) return;
        if (!canAccessProject(projectId)) {
            log.warn("Team access denied: user cannot access project {}", projectId);
            throw new AccessDeniedException("You do not have access to this project's data.");
        }
    }

    public void assertTestCaseAccess(UUID testCaseId) {
        if (testCaseId == null) return;
        Optional<UUID> restrict = currentUserTeamIdIfRestricted();
        if (restrict.isEmpty()) return;
        TestCase tc = testCaseRepository.findById(testCaseId).orElse(null);
        if (tc == null || tc.getProject() == null) return;
        if (!canAccessProject(tc.getProject())) {
            log.warn("Team access denied: user cannot access test case {}", testCaseId);
            throw new AccessDeniedException("You do not have access to this test case.");
        }
    }

    public void assertDefectAccess(UUID defectId) {
        if (defectId == null) return;
        Optional<UUID> restrict = currentUserTeamIdIfRestricted();
        if (restrict.isEmpty()) return;
        Defect d = defectRepository.findById(defectId).orElse(null);
        if (d == null || d.getProject() == null) return;
        if (!canAccessProject(d.getProject())) {
            log.warn("Team access denied: user cannot access defect {}", defectId);
            throw new AccessDeniedException("You do not have access to this defect.");
        }
    }

    public void assertExecutionAccess(UUID executionId) {
        if (executionId == null) return;
        Optional<UUID> restrict = currentUserTeamIdIfRestricted();
        if (restrict.isEmpty()) return;
        TestExecution ex = testExecutionRepository.findById(executionId).orElse(null);
        if (ex == null || ex.getProject() == null) return;
        if (!canAccessProject(ex.getProject())) {
            log.warn("Team access denied: user cannot access execution {}", executionId);
            throw new AccessDeniedException("You do not have access to this execution record.");
        }
    }
}
