package com.testmgmt.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testmgmt.dto.request.WorkflowDTOs.CreateProjectRequest;
import com.testmgmt.dto.response.ResponseDTOs.*;
import com.testmgmt.dto.response.ResponseDTOs.ProjectResponse;
import com.testmgmt.entity.Project;
import com.testmgmt.entity.User;
import com.testmgmt.exception.ResourceNotFoundException;
import com.testmgmt.repository.ProjectRepository;
import com.testmgmt.repository.TeamRepository;
import com.testmgmt.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository    userRepository;
    private final TeamRepository    teamRepository;
    private final TeamAccessGuard   teamAccessGuard;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerEmail));

        Project parent = null;
        if (request.getParentProjectId() != null) {
            parent = projectRepository.findById(request.getParentProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Project",
                            request.getParentProjectId()));
        }

        // Determine team ownership: explicit teamId, else inherit from parent, else owner's team
        UUID teamId = request.getTeamId();
        if (teamId == null && parent != null) teamId = parent.getTeamId();
        if (teamId == null) teamId = owner.getTeamId();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .parentProject(parent)
                .teamId(teamId)
                .active(true)
                .build();

        return toResponseWithTeam(projectRepository.save(project), true);
    }

    /** Returns root-level projects with their sub-projects nested inside, filtered by team. */
    public List<ProjectResponse> findAll() {
        UUID filterTeamId = currentUserTeamIdIfRestricted();
        return projectRepository.findByActiveTrueAndParentProjectIsNull()
                .stream()
                .filter(p -> visibleToTeam(p, filterTeamId))
                .map(p -> toResponseWithTeam(p, true))
                .toList();
    }

    /** Returns all projects flat (root + sub) — used for dropdowns, filtered by team. */
    public List<ProjectResponse> findAllFlat() {
        UUID filterTeamId = currentUserTeamIdIfRestricted();
        return projectRepository.findByActiveTrue()
                .stream()
                .filter(p -> visibleToTeam(p, filterTeamId))
                .map(p -> toResponseWithTeam(p, false))
                .toList();
    }

    public List<ProjectResponse> findSubProjects(UUID parentId) {
        Project parent = projectRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", parentId));
        UUID filterTeamId = currentUserTeamIdIfRestricted();
        return projectRepository.findByActiveTrueAndParentProject(parent)
                .stream()
                .filter(p -> visibleToTeam(p, filterTeamId))
                .map(p -> toResponseWithTeam(p, false))
                .toList();
    }

    public ProjectResponse findById(UUID id) {
        teamAccessGuard.assertProjectAccess(id);
        return toResponseWithTeam(projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id)), true);
    }

    @Transactional
    public ProjectResponse update(UUID id, CreateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        if (request.getTeamId() != null) {
            project.setTeamId(request.getTeamId());
        }

        if (request.getParentProjectId() != null) {
            Project parent = projectRepository.findById(request.getParentProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Project",
                            request.getParentProjectId()));
            project.setParentProject(parent);
        }
        return toResponseWithTeam(projectRepository.save(project), true);
    }

    @Transactional
    public void deactivate(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        project.setActive(false);
        // Also deactivate sub-projects
        projectRepository.findByActiveTrueAndParentProject(project)
                .forEach(sub -> { sub.setActive(false); projectRepository.save(sub); });
        projectRepository.save(project);
    }

    // ── Team-based access control helpers ────────────────────────────────────

    /**
     * Returns the current user's teamId if projects should be filtered for them,
     * or null if they should see all projects (delegates to {@link TeamAccessGuard}).
     */
    private UUID currentUserTeamIdIfRestricted() {
        return teamAccessGuard.currentUserTeamIdIfRestricted().orElse(null);
    }

    /** Project visible if: no filter, project has no team (shared), or teams match. */
   private static boolean visibleToTeam(Project p, UUID filterTeamId) {
    if (filterTeamId == null) {
        return true; // Admin
    }
    return filterTeamId.equals(p.getTeamId());
}

    private ProjectResponse toResponseWithTeam(Project p, boolean includeSubProjects) {
        ProjectResponse r = toResponse(p, includeSubProjects);
        if (r != null && p.getTeamId() != null) {
            r.setTeamId(p.getTeamId());
            teamRepository.findById(p.getTeamId())
                    .ifPresent(t -> r.setTeamName(t.getName()));
        }
        return r;
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static ProjectResponse toResponse(Project p, boolean includeSubProjects) {
        if (p == null) return null;
        List<ProjectResponse> subs = Collections.emptyList();
        if (includeSubProjects && p.getSubProjects() != null && !p.getSubProjects().isEmpty()) {
            subs = p.getSubProjects().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActive()))
                    .map(s -> toResponse(s, false))
                    .toList();
        }
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .owner(AuthService.toUserResponse(p.getOwner()))
                .parentProjectId(p.getParentProject() != null ? p.getParentProject().getId() : null)
                .parentProjectName(p.getParentProject() != null ? p.getParentProject().getName() : null)
                .teamId(p.getTeamId())
                .subProjects(subs)
                .build();
    }

    /** Backward-compat overload */
    public static ProjectResponse toResponse(Project p) {
        return toResponse(p, false);
    }
}
