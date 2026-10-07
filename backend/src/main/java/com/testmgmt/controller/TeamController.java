package com.testmgmt.controller;

import com.testmgmt.dto.response.ResponseDTOs.ApiResponse;
import com.testmgmt.service.TeamService;
import com.testmgmt.service.TeamService.TeamRequest;
import com.testmgmt.service.TeamService.TeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * Get all teams for current tenant
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getAllTeams()));
    }

    /**
     * Get teams with pagination
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<TeamResponse>>> getTeamsPaged(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsPaged(pageable)));
    }

    /**
     * Get team by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamById(id)));
    }

    /**
     * Create a new team
     */
    @PostMapping
    // OLD: @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PreAuthorize("@superAdminGuard.check()")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(teamService.createTeam(request)));
    }

    /**
     * Update team
     */
    @PutMapping("/{id}")
    // OLD: @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PreAuthorize("@superAdminGuard.check()")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable UUID id,
            @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success(teamService.updateTeam(id, request)));
    }

    /**
     * Delete team (soft delete)
     */
    @DeleteMapping("/{id}")
    // OLD: @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("@superAdminGuard.check()")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get teams by department
     */
    @GetMapping("/by-department/{department}")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByDepartment(
            @PathVariable String department) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByDepartment(department)));
    }

    /**
     * Get teams by channel
     */
    @GetMapping("/by-channel/{channel}")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsByChannel(
            @PathVariable String channel) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsByChannel(channel)));
    }

    /**
     * Search teams
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> searchTeams(
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(teamService.searchTeams(q)));
    }

    /**
     * Get team member count
     */
    @GetMapping("/{id}/member-count")
    public ResponseEntity<ApiResponse<Long>> getTeamMemberCount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamMemberCount(id)));
    }

    // ── Team Member Management ───────────────────────────────────────────────

    /** List active members of a team. */
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<com.testmgmt.dto.response.ResponseDTOs.UserResponse>>>
            getTeamMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamMembers(id)));
    }

    /** Add one or more users to this team. Body: {"userIds":["uuid1","uuid2"]} */
    @PostMapping("/{id}/members")
    // OLD: @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PreAuthorize("@superAdminGuard.check()")
    public ResponseEntity<ApiResponse<List<com.testmgmt.dto.response.ResponseDTOs.UserResponse>>>
            addTeamMembers(@PathVariable UUID id, @RequestBody AddMembersRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                teamService.addTeamMembers(id, request.getUserIds())));
    }

    /** Remove a user from this team. */
    @DeleteMapping("/{id}/members/{userId}")
    // OLD: @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PreAuthorize("@superAdminGuard.check()")
    public ResponseEntity<ApiResponse<Void>> removeTeamMember(
            @PathVariable UUID id, @PathVariable UUID userId) {
        teamService.removeTeamMember(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @lombok.Data
    public static class AddMembersRequest {
        private List<UUID> userIds;
    }
}