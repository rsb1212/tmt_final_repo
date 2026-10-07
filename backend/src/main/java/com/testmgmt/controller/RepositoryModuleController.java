package com.testmgmt.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.testmgmt.dto.response.ResponseDTOs.ApiResponse;
import com.testmgmt.entity.RepositoryModule;
import com.testmgmt.entity.RepositoryNode;
import com.testmgmt.entity.RepositoryNodeDocument;
import com.testmgmt.entity.User;
import com.testmgmt.service.RepositoryModuleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/repository-modules")
@RequiredArgsConstructor
@Slf4j
public class RepositoryModuleController {

    private final RepositoryModuleService repositoryModuleService;

    // ==================== MODULE ENDPOINTS ====================

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getModuleTree() {
        Map<String, Object> tree = repositoryModuleService.getModuleTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getAllModules() {
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getAllModules()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getModuleById(@PathVariable UUID id) {
        return repositoryModuleService.getModuleById(id)
                .<ResponseEntity<ApiResponse<?>>>map(module -> ResponseEntity.ok(ApiResponse.success(module)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RepositoryModule>> createModule(@RequestBody Map<String, String> request) {
        RepositoryModule module = repositoryModuleService.createModule(
                request.get("name"),
                request.get("description"),
                request.get("icon"),
                request.get("color")
        );
        return ResponseEntity.ok(ApiResponse.success(module));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RepositoryModule>> updateModule(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        RepositoryModule module = repositoryModuleService.updateModule(
                id,
                request.get("name"),
                request.get("description"),
                request.get("icon"),
                request.get("color")
        );
        return ResponseEntity.ok(ApiResponse.success(module));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModule(@PathVariable UUID id) {
        repositoryModuleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== NODE ENDPOINTS ====================

    @GetMapping("/{moduleId}/nodes")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getRootNodes(@PathVariable UUID moduleId) {
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getRootNodes(moduleId)));
    }

    @GetMapping("/nodes/{nodeId}/children")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getChildNodes(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getChildNodes(nodeId)));
    }

    @PostMapping("/nodes")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<RepositoryNode>> createNode(@RequestBody Map<String, Object> request) {
        UUID moduleId = UUID.fromString((String) request.get("moduleId"));
        UUID parentId = request.get("parentId") != null 
                ? UUID.fromString((String) request.get("parentId")) 
                : null;
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String icon = (String) request.get("icon");
        String nodeType = (String) request.get("nodeType");
        boolean allowAddNew = request.get("allowAddNew") != null 
                ? (Boolean) request.get("allowAddNew") 
                : false;

        RepositoryNode node = repositoryModuleService.createNode(
                moduleId, parentId, name, description, icon, nodeType, allowAddNew);
        return ResponseEntity.ok(ApiResponse.success(node));
    }

    @PutMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<RepositoryNode>> updateNode(
            @PathVariable UUID nodeId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String icon = (String) request.get("icon");
        boolean allowAddNew = request.get("allowAddNew") != null 
                ? (Boolean) request.get("allowAddNew") 
                : false;
        RepositoryNode node = repositoryModuleService.updateNode(nodeId, name, description, icon, allowAddNew);
        return ResponseEntity.ok(ApiResponse.success(node));
    }
    @DeleteMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteNode(@PathVariable UUID nodeId) {
        repositoryModuleService.deleteNode(nodeId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    // ==================== DOCUMENT ENDPOINTS ====================
    @GetMapping("/nodes/{nodeId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getDocuments(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getDocuments(nodeId)));
    }
    @PostMapping("/nodes/{nodeId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER')")
    public ResponseEntity<ApiResponse<RepositoryNodeDocument>> uploadDocument(
            @PathVariable UUID nodeId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String relativePath,
            @AuthenticationPrincipal User user) {
        try {
            RepositoryNodeDocument document = repositoryModuleService.uploadDocument(
                    nodeId, projectId, file, description, relativePath, user);
            return ResponseEntity.ok(ApiResponse.success(document));
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
        }
    }

    @PutMapping("/documents/{documentId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME')")
    public ResponseEntity<ApiResponse<Void>> archiveDocument(@PathVariable UUID documentId) {
        repositoryModuleService.archiveDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID documentId) {
        repositoryModuleService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== CONFLUENCE LIVING PAGE ENDPOINTS ====================

    @GetMapping("/nodes/{nodeId}/page")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPage(
            @PathVariable UUID nodeId,
            org.springframework.security.core.Authentication auth) {
        String username = auth != null ? auth.getName() : "Anonymous";
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getPage(nodeId, username)));
    }

    @PutMapping("/nodes/{nodeId}/page")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> savePage(
            @PathVariable UUID nodeId,
            @RequestBody Map<String, Object> pageData,
            org.springframework.security.core.Authentication auth) {
        String username = auth != null ? auth.getName() : "Anonymous";
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.savePage(nodeId, pageData, username)));
    }

    @GetMapping("/nodes/{nodeId}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getVersions(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getPageVersions(nodeId)));
    }

    @PostMapping("/nodes/{nodeId}/restore-version")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restoreVersion(
            @PathVariable UUID nodeId,
            @RequestParam int version,
            org.springframework.security.core.Authentication auth) {
        String username = auth != null ? auth.getName() : "Anonymous";
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.restorePageVersion(nodeId, version, username)));
    }

    @GetMapping("/nodes/{nodeId}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER','VIEWER')")
    public ResponseEntity<ApiResponse<?>> getComments(@PathVariable UUID nodeId) {
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getPageComments(nodeId)));
    }

    @PostMapping("/nodes/{nodeId}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
            @PathVariable UUID nodeId,
            @RequestBody Map<String, String> body,
            org.springframework.security.core.Authentication auth) {
        String username = auth != null ? auth.getName() : "Anonymous";
        String role = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "TESTER";
        String text = body.get("text");
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.addPageComment(nodeId, text, username, role)));
    }

    @PostMapping("/nodes/{nodeId}/star")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleStar(
            @PathVariable UUID nodeId,
            org.springframework.security.core.Authentication auth) {
        String username = auth != null ? auth.getName() : "Anonymous";
        boolean starred = repositoryModuleService.toggleStar(nodeId, username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("starred", starred)));
    }

    @GetMapping("/stars")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','SME','TESTER')")
    public ResponseEntity<ApiResponse<?>> getStars(org.springframework.security.core.Authentication auth) {
        String username = auth != null ? auth.getName() : "Anonymous";
        return ResponseEntity.ok(ApiResponse.success(repositoryModuleService.getUserStars(username)));
    }

    // ==================== SEED ENDPOINT (Admin only) ====================

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> seedModules() {
        repositoryModuleService.seedDefaultModules();
        return ResponseEntity.ok(ApiResponse.success("Modules seeded successfully"));
    }
}
