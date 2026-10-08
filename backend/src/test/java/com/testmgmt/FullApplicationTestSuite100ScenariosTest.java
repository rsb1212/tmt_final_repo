package com.testmgmt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testmgmt.dto.request.AuthDTOs.LoginRequest;
import com.testmgmt.dto.request.AuthDTOs.RegisterRequest;
import com.testmgmt.dto.request.CallDTOs.CreateCallRequest;
import com.testmgmt.dto.request.ExecutionDTOs.StepResultRequest;
import com.testmgmt.dto.request.ExecutionDTOs.SubmitExecutionRequest;
import com.testmgmt.dto.request.ExecutionDTOs.UpdateExecutionRequest;
import com.testmgmt.dto.request.WorkflowDTOs.*;
import com.testmgmt.entity.Module;
import com.testmgmt.entity.Project;
import com.testmgmt.entity.TestCase;
import com.testmgmt.entity.User;
import com.testmgmt.enums.*;
import com.testmgmt.entity.TestCaseAssignment;
import com.testmgmt.repository.TestCaseAssignmentRepository;
import com.testmgmt.repository.ModuleRepository;
import com.testmgmt.repository.ProjectRepository;
import com.testmgmt.repository.TestCaseRepository;
import com.testmgmt.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 100 Comprehensive End-to-End Test Scenarios for Test Genii (TMT) Platform.
 * Covers all 10 core functional areas:
 * 1. Authentication & Security (001-010)
 * 2. Project & Sub-project Hierarchy (011-020)
 * 3. Repository Modules & Node Hierarchy (021-030)
 * 4. Test Case Authoring, Steps & Pagination (031-045)
 * 5. Workflow, SME Reviews, Approvals & Assignments (046-058)
 * 6. Test Executions & Step Results (059-070)
 * 7. Defects & Bug Tracking Lifecycle (071-080)
 * 8. Requirements & Global Search (081-088)
 * 9. Tester Releases, QA Calls & Notifications (089-094)
 * 10. Dashboards, Productivity, Multi-Tenancy & Health (095-100)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class FullApplicationTestSuite100ScenariosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private TestCaseAssignmentRepository testCaseAssignmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Shared state across sequential tests
    private static UUID adminUserId;
    private static UUID managerUserId;
    private static UUID testerUserId;
    private static UUID smeUserId;
    private static UUID rootProjectId;
    private static UUID secondProjectId;
    private static UUID subProjectId;
    private static UUID repositoryModuleId;
    private static UUID repositoryNodeId;
    private static UUID testCaseId1;
    private static UUID testCaseId2;
    private static UUID testStepId1;
    private static UUID executionId;
    private static UUID defectId;
    private static UUID requirementId;
    private static UUID releaseRequestId;
    private static UUID qaCallId;
    private static UUID tenantId;

    @BeforeAll
    void setupSharedUsersAndData() {
        // Seed users with different roles if not present
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = userRepository.save(User.builder()
                    .email("admin@example.com")
                    .username("admin")
                    .fullName("Admin User")
                    .passwordHash(passwordEncoder.encode("Admin@1234"))
                    .role(UserRole.ADMIN)
                    .active(true)
                    .build());
            adminUserId = admin.getId();
        } else {
            adminUserId = userRepository.findByEmail("admin@example.com").get().getId();
        }

        if (!userRepository.existsByEmail("manager@example.com")) {
            User manager = userRepository.save(User.builder()
                    .email("manager@example.com")
                    .username("manager")
                    .fullName("Manager User")
                    .passwordHash(passwordEncoder.encode("Manager@1234"))
                    .role(UserRole.MANAGER)
                    .active(true)
                    .build());
            managerUserId = manager.getId();
        } else {
            managerUserId = userRepository.findByEmail("manager@example.com").get().getId();
        }

        if (!userRepository.existsByEmail("tester@example.com")) {
            User tester = userRepository.save(User.builder()
                    .email("tester@example.com")
                    .username("tester")
                    .fullName("Tester User")
                    .passwordHash(passwordEncoder.encode("Tester@1234"))
                    .role(UserRole.TESTER)
                    .active(true)
                    .build());
            testerUserId = tester.getId();
        } else {
            testerUserId = userRepository.findByEmail("tester@example.com").get().getId();
        }

        if (!userRepository.existsByEmail("sme@example.com")) {
            User sme = userRepository.save(User.builder()
                    .email("sme@example.com")
                    .username("smeuser")
                    .fullName("SME User")
                    .passwordHash(passwordEncoder.encode("Sme@12345"))
                    .role(UserRole.SME)
                    .active(true)
                    .build());
            smeUserId = sme.getId();
        } else {
            smeUserId = userRepository.findByEmail("sme@example.com").get().getId();
        }
    }

    // =========================================================================
    // MODULE 1: AUTHENTICATION & SECURITY (001 - 010)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("TS-001: Register new tester with valid details returns 200 OK")
    void test001_register_new_tester_success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newtester");
        req.setEmail("newtester@example.com");
        req.setPassword("Password@123");
        req.setFullName("New Tester");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("newtester@example.com"));
    }

    @Test
    @Order(2)
    @DisplayName("TS-002: Register with already registered email returns 409 Conflict")
    void test002_register_duplicate_email_fails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("anotheruser");
        req.setEmail("newtester@example.com");
        req.setPassword("Password@123");
        req.setFullName("Duplicate Tester");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("TS-003: Register with short password (< 8 chars) returns 400 Bad Request")
    void test003_register_short_password_fails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser");
        req.setEmail("shortpass@example.com");
        req.setPassword("short");
        req.setFullName("Short Pass");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("TS-004: Register with invalid email format returns 400 Bad Request")
    void test004_register_invalid_email_fails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("validuser2");
        req.setEmail("invalid-email-address");
        req.setPassword("ValidPassword@123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("TS-005: Register with username < 3 characters returns 400 Bad Request")
    void test005_register_short_username_fails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("ab");
        req.setEmail("shortuser@example.com");
        req.setPassword("ValidPassword@123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("TS-006: Login with valid credentials returns 200 OK and JWT token")
    void test006_login_valid_credentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@example.com");
        req.setPassword("Admin@1234");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    @Order(7)
    @DisplayName("TS-007: Login with wrong password returns 401 Unauthorized")
    void test007_login_wrong_password_fails() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@example.com");
        req.setPassword("WrongPassword@999");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(8)
    @DisplayName("TS-008: Login with non-existent user returns 401 Unauthorized")
    void test008_login_nonexistent_user_fails() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("SomePassword@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    @DisplayName("TS-009: Unauthenticated request to protected endpoint returns 401/403")
    void test009_unauthenticated_request_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Project\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("TS-010: Authenticated user views their profile (/api/v1/users/me)")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test010_get_current_user_profile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    // =========================================================================
    // MODULE 2: PROJECT & SUB-PROJECT MANAGEMENT (011 - 020)
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("TS-011: Admin creates a root project returns 201 Created")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test011_create_root_project_as_admin() throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Core Banking System");
        req.setDescription("Next-Gen Retail & Corporate Banking Platform");

        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Core Banking System"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        rootProjectId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(rootProjectId);
    }

    @Test
    @Order(12)
    @DisplayName("TS-012: Manager creates a second root project returns 201 Created")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test012_create_second_project_as_manager() throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Mobile Banking App");
        req.setDescription("iOS and Android Customer App");

        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        secondProjectId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(secondProjectId);
    }

    @Test
    @Order(13)
    @DisplayName("TS-013: Tester role cannot create a project (403 Forbidden)")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test013_tester_create_project_forbidden() throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Tester Unauthorized Project");

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    @DisplayName("TS-014: Create project with blank name returns 400 Bad Request")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test014_create_project_blank_name_fails() throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("");

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(15)
    @DisplayName("TS-015: Admin creates sub-project under root project returns 201 Created")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test015_create_subproject_under_parent() throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Payment Gateway Integration");
        req.setDescription("Sub-module for UPI and SWIFT payments");
        req.setParentProjectId(rootProjectId);

        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Payment Gateway Integration"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        subProjectId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(subProjectId);
    }

    @Test
    @Order(16)
    @DisplayName("TS-016: List root projects nested returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test016_list_all_projects_nested() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(17)
    @DisplayName("TS-017: List flat projects for dropdowns returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test017_list_all_projects_flat() throws Exception {
        mockMvc.perform(get("/api/v1/projects/flat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(18)
    @DisplayName("TS-018: Get project by valid ID returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test018_get_project_by_id() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + rootProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(rootProjectId.toString()))
                .andExpect(jsonPath("$.data.name").value("Core Banking System"));
    }

    @Test
    @Order(19)
    @DisplayName("TS-019: Get project by non-existent UUID returns 404 Not Found")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test019_get_nonexistent_project_returns_404() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(20)
    @DisplayName("TS-020: Update project details returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test020_update_project_details() throws Exception {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Core Banking System - Updated");
        req.setDescription("Updated description for Core Banking");

        mockMvc.perform(put("/api/v1/projects/" + rootProjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Core Banking System - Updated"));
    }

    // =========================================================================
    // MODULE 3: REPOSITORY MODULES & NODE HIERARCHY (021 - 030)
    // =========================================================================

    @Test
    @Order(21)
    @DisplayName("TS-021: Get repository module tree returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test021_get_repository_module_tree() throws Exception {
        mockMvc.perform(get("/api/v1/repository-modules/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(22)
    @DisplayName("TS-022: List all repository modules returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test022_list_all_repository_modules() throws Exception {
        mockMvc.perform(get("/api/v1/repository-modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(23)
    @DisplayName("TS-023: Create repository module returns 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test023_create_repository_module() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("name", "Account Management");
        req.put("description", "Repository module for account lifecycle");

        MvcResult result = mockMvc.perform(post("/api/v1/repository-modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Account Management"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        repositoryModuleId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(repositoryModuleId);
    }

    @Test
    @Order(24)
    @DisplayName("TS-024: Get repository module by valid ID returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test024_get_repository_module_by_id() throws Exception {
        mockMvc.perform(get("/api/v1/repository-modules/" + repositoryModuleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(repositoryModuleId.toString()));
    }

    @Test
    @Order(25)
    @DisplayName("TS-025: Get repository module with invalid UUID returns 404 Not Found")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test025_get_nonexistent_repository_module() throws Exception {
        mockMvc.perform(get("/api/v1/repository-modules/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(26)
    @DisplayName("TS-026: Update repository module returns 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test026_update_repository_module() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("name", "Account & Customer Management");
        req.put("description", "Expanded module definition");

        mockMvc.perform(put("/api/v1/repository-modules/" + repositoryModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Account & Customer Management"));
    }

    @Test
    @Order(27)
    @DisplayName("TS-027: Create repository node folder inside module returns 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test027_create_repository_node_folder() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("moduleId", repositoryModuleId.toString());
        req.put("name", "Savings Accounts");
        req.put("nodeType", "FOLDER");

        MvcResult result = mockMvc.perform(post("/api/v1/repository-modules/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        repositoryNodeId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(repositoryNodeId);
    }

    @Test
    @Order(28)
    @DisplayName("TS-028: List repository nodes for module returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test028_list_repository_nodes_by_module() throws Exception {
        mockMvc.perform(get("/api/v1/repository-modules/" + repositoryModuleId + "/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(29)
    @DisplayName("TS-029: Get child repository nodes by parent ID returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test029_get_repository_node_by_id() throws Exception {
        mockMvc.perform(get("/api/v1/repository-modules/nodes/" + repositoryNodeId + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(30)
    @DisplayName("TS-030: Update repository node name returns 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test030_update_repository_node() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("name", "High-Yield Savings Accounts");

        mockMvc.perform(put("/api/v1/repository-modules/nodes/" + repositoryNodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("High-Yield Savings Accounts"));
    }

    // =========================================================================
    // MODULE 4: TEST CASE AUTHORING & STEPS (031 - 045)
    // =========================================================================

    @Test
    @Order(31)
    @DisplayName("TS-031: Tester creates test case with structured steps returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test031_create_test_case_with_steps() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("Verify valid user login with OTP");
        req.setDescription("User enters credentials and completes two-factor authentication");
        req.setPreconditions("User account is active with 2FA enabled");
        req.setProjectId(rootProjectId);
        req.setPriority(Priority.HIGH);

        StepRequest s1 = new StepRequest();
        s1.setStepNumber(1);
        s1.setStepAction("Navigate to login portal and enter username and password");
        s1.setExpectedResult("Credentials validated, OTP prompt appears");

        StepRequest s2 = new StepRequest();
        s2.setStepNumber(2);
        s2.setStepAction("Enter valid 6-digit OTP received via SMS");
        s2.setExpectedResult("Dashboard opens with user greeting");

        req.setSteps(List.of(s1, s2));

        MvcResult result = mockMvc.perform(post("/api/v1/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Verify valid user login with OTP"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        testCaseId1 = UUID.fromString(rootNode.path("data").path("id").asText());
        testStepId1 = UUID.fromString(rootNode.path("data").path("steps").get(0).path("id").asText());
        assertNotNull(testCaseId1);
        assertNotNull(testStepId1);
    }

    @Test
    @Order(32)
    @DisplayName("TS-032: Create test case without title returns 400 Bad Request")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test032_create_test_case_missing_title_fails() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("");
        req.setProjectId(rootProjectId);
        req.setPriority(Priority.MEDIUM);

        mockMvc.perform(post("/api/v1/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(33)
    @DisplayName("TS-033: Create test case without projectId returns 400 Bad Request")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test033_create_test_case_missing_project_fails() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("Orphan Test Case");
        req.setPriority(Priority.LOW);

        mockMvc.perform(post("/api/v1/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(34)
    @DisplayName("TS-034: Create test case without priority returns 400 Bad Request")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test034_create_test_case_missing_priority_fails() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("No Priority Test Case");
        req.setProjectId(rootProjectId);

        mockMvc.perform(post("/api/v1/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(35)
    @DisplayName("TS-035: Duplicate test case title in the same project returns 409 Conflict")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test035_create_duplicate_title_test_case_fails() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("Verify valid user login with OTP");
        req.setProjectId(rootProjectId);
        req.setPriority(Priority.HIGH);

        mockMvc.perform(post("/api/v1/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(36)
    @DisplayName("TS-036: Tester creates second test case in project returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test036_create_second_test_case() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("Verify funds transfer between internal accounts");
        req.setDescription("Checking transfer from Savings to Checking account");
        req.setProjectId(rootProjectId);
        req.setPriority(Priority.CRITICAL);

        MvcResult result = mockMvc.perform(post("/api/v1/testcases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        testCaseId2 = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(testCaseId2);
    }

    @Test
    @Order(37)
    @DisplayName("TS-037: Get test case by valid ID returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test037_get_test_case_by_id() throws Exception {
        mockMvc.perform(get("/api/v1/testcases/" + testCaseId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(testCaseId1.toString()))
                .andExpect(jsonPath("$.data.title").value("Verify valid user login with OTP"));
    }

    @Test
    @Order(38)
    @DisplayName("TS-038: Get test case by non-existent UUID returns 404 Not Found")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test038_get_nonexistent_test_case() throws Exception {
        mockMvc.perform(get("/api/v1/testcases/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(39)
    @DisplayName("TS-039: Filter test cases by projectId returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test039_list_test_cases_filter_by_project() throws Exception {
        mockMvc.perform(get("/api/v1/testcases")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(40)
    @DisplayName("TS-040: Filter test cases by status DRAFT returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test040_list_test_cases_filter_by_status() throws Exception {
        mockMvc.perform(get("/api/v1/testcases")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(41)
    @DisplayName("TS-041: List test cases with custom page size returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test041_list_test_cases_pagination() throws Exception {
        mockMvc.perform(get("/api/v1/testcases")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    @Order(42)
    @DisplayName("TS-042: Update test case content via standard PUT returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test042_update_test_case_content() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("Verify valid user login with OTP - Updated");
        req.setDescription("Updated description for login scenario");
        req.setProjectId(rootProjectId);
        req.setPriority(Priority.CRITICAL);

        mockMvc.perform(put("/api/v1/testcases/" + testCaseId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Verify valid user login with OTP - Updated"))
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"));
    }

    @Test
    @Order(43)
    @DisplayName("TS-043: Edit test case via workflow /edit endpoint returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test043_edit_test_case_via_workflow() throws Exception {
        CreateTestCaseRequest req = new CreateTestCaseRequest();
        req.setTitle("Verify valid user login with OTP");
        req.setDescription("Restored title back to original");
        req.setProjectId(rootProjectId);
        req.setPriority(Priority.HIGH);

        mockMvc.perform(put("/api/v1/testcases/" + testCaseId1 + "/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Verify valid user login with OTP"));
    }

    @Test
    @Order(44)
    @DisplayName("TS-044: Download blank Excel import template returns 200 OK with excel headers")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test044_download_excel_template() throws Exception {
        mockMvc.perform(get("/api/v1/testcases/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("testcase-import-template.xlsx")));
    }

    @Test
    @Order(45)
    @DisplayName("TS-045: Export test cases for project as Excel returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test045_export_test_cases_excel() throws Exception {
        mockMvc.perform(get("/api/v1/testcases/export")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("test-cases-")));
    }

    // =========================================================================
    // MODULE 5: WORKFLOW, SME REVIEW & ASSIGNMENTS (046 - 058)
    // =========================================================================

    @Test
    @Order(46)
    @DisplayName("TS-046: Forward test case to SME for review returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test046_forward_to_sme() throws Exception {
        mockMvc.perform(patch("/api/v1/testcases/" + testCaseId1 + "/forward-sme")
                        .param("smeId", smeUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_SME_REVIEW"));
    }

    @Test
    @Order(47)
    @DisplayName("TS-047: SME views review queue returns 200 OK with pending cases")
    @WithMockUser(username = "sme@example.com", roles = {"SME"})
    void test047_get_sme_queue() throws Exception {
        mockMvc.perform(get("/api/v1/testcases/sme-queue")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(48)
    @DisplayName("TS-048: SME requests changes on test case returns 200 OK")
    @WithMockUser(username = "sme@example.com", roles = {"SME"})
    void test048_sme_request_changes() throws Exception {
        RequestChangesRequest req = new RequestChangesRequest();
        req.setComment("Please add negative test steps for invalid OTP");

        mockMvc.perform(post("/api/v1/testcases/" + testCaseId1 + "/request-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @Order(49)
    @DisplayName("TS-049: Tester re-forwards test case to SME after changes returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test049_tester_reforward_to_sme() throws Exception {
        mockMvc.perform(patch("/api/v1/testcases/" + testCaseId1 + "/forward-sme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_SME_REVIEW"));
    }

    @Test
    @Order(50)
    @DisplayName("TS-050: SME approves test case via review endpoint returns 200 OK")
    @WithMockUser(username = "sme@example.com", roles = {"SME"})
    void test050_sme_approve_test_case() throws Exception {
        SMEReviewRequest req = new SMEReviewRequest();
        req.setAction("APPROVE");
        req.setReviewNote("All security scenarios verified");

        mockMvc.perform(put("/api/v1/testcases/" + testCaseId1 + "/sme-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SME_APPROVED"));
    }

    @Test
    @Order(51)
    @DisplayName("TS-051: Non-SME cannot execute SME review endpoint (403 Forbidden)")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test051_non_sme_cannot_review() throws Exception {
        SMEReviewRequest req = new SMEReviewRequest();
        req.setAction("APPROVE");

        mockMvc.perform(put("/api/v1/testcases/" + testCaseId2 + "/sme-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(52)
    @DisplayName("TS-052: SME bulk approves test cases returns 200 OK")
    @WithMockUser(username = "sme@example.com", roles = {"SME"})
    void test052_sme_bulk_approve() throws Exception {
        // Forward testCase2 to PENDING_SME_REVIEW first
        TestCase tc2 = testCaseRepository.findById(testCaseId2).get();
        tc2.setStatus(TestStatus.PENDING_SME_REVIEW);
        testCaseRepository.save(tc2);

        BulkApproveRequest req = new BulkApproveRequest();
        req.setTestCaseIds(List.of(testCaseId2));
        req.setComment("Bulk approved during sprint review");

        mockMvc.perform(post("/api/v1/testcases/bulk-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(53)
    @DisplayName("TS-053: Manager assigns test case to tester returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test053_manager_assign_cases() throws Exception {
        AssignCasesRequest req = new AssignCasesRequest();
        req.setTestCaseIds(List.of(testCaseId1));
        req.setAssignedToUserId(testerUserId);

        mockMvc.perform(post("/api/v1/testcases/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].assignedTo.id").value(testerUserId.toString()));
    }

    @Test
    @Order(54)
    @DisplayName("TS-054: Manager reassigns test case to another tester with reason returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test054_manager_reassign_case() throws Exception {
        ReassignRequest req = new ReassignRequest();
        req.setNewAssigneeId(testerUserId);
        req.setReason("Rebalancing sprint workload");

        mockMvc.perform(patch("/api/v1/testcases/" + testCaseId1 + "/reassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedTo.id").value(testerUserId.toString()));
    }

    @Test
    @Order(55)
    @DisplayName("TS-055: Send PASSED test case to UAT pending returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test055_send_to_uat_pending() throws Exception {
        // Set testcase status to PASSED so UAT can be triggered
        TestCase tc1 = testCaseRepository.findById(testCaseId1).get();
        tc1.setStatus(TestStatus.PASSED);
        testCaseRepository.save(tc1);

        mockMvc.perform(patch("/api/v1/testcases/" + testCaseId1 + "/send-uat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UAT_PENDING"));
    }

    @Test
    @Order(56)
    @DisplayName("TS-056: Start UAT execution (UAT_PENDING -> UAT_IN_PROGRESS) returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test056_start_uat() throws Exception {
        mockMvc.perform(patch("/api/v1/testcases/" + testCaseId1 + "/uat-start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UAT_IN_PROGRESS"));
    }

    @Test
    @Order(57)
    @DisplayName("TS-057: Pass UAT test case (UAT_IN_PROGRESS -> UAT_PASSED) returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test057_pass_uat() throws Exception {
        mockMvc.perform(patch("/api/v1/testcases/" + testCaseId1 + "/uat-pass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UAT_PASSED"));
    }

    @Test
    @Order(58)
    @DisplayName("TS-058: Clone test case to another project returns 201/200 OK with DRAFT copy")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test058_clone_test_case() throws Exception {
        CloneTestCaseRequest req = new CloneTestCaseRequest();
        req.setTargetProjectId(secondProjectId);

        mockMvc.perform(post("/api/v1/testcases/" + testCaseId1 + "/clone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.project.id").value(secondProjectId.toString()));
    }

    // =========================================================================
    // MODULE 6: TEST EXECUTIONS & STEP RESULTS (059 - 070)
    // =========================================================================

    @Test
    @Order(59)
    @DisplayName("TS-059: Submit execution with PASSED result returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test059_submit_execution_passed() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setTestCaseId(testCaseId1);
        req.setResult(ExecResult.PASSED);
        req.setActualResult("All steps completed successfully");
        req.setBuildVersion("v2.1.0-RC1");
        req.setDurationSeconds(120);

        MvcResult result = mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("PASSED"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        executionId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(executionId);
    }

    @Test
    @Order(60)
    @DisplayName("TS-060: Submit execution with FAILED result returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test060_submit_execution_failed() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setTestCaseId(testCaseId2);
        req.setResult(ExecResult.FAILED);
        req.setActualResult("Transaction timed out with HTTP 504");
        req.setDefectRef("BUG-101");
        req.setDurationSeconds(45);

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("FAILED"))
                .andExpect(jsonPath("$.data.defectRef").value("BUG-101"));
    }

    @Test
    @Order(61)
    @DisplayName("TS-061: Submit execution with BLOCKED result returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test061_submit_execution_blocked() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setTestCaseId(testCaseId1);
        req.setResult(ExecResult.BLOCKED);
        req.setNotes("Payment gateway down for maintenance in SIT");

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("BLOCKED"));
    }

    @Test
    @Order(62)
    @DisplayName("TS-062: Submit execution with SKIPPED result returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test062_submit_execution_skipped() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setTestCaseId(testCaseId1);
        req.setResult(ExecResult.SKIPPED);
        req.setNotes("Out of scope for current sprint");

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.result").value("SKIPPED"));
    }

    @Test
    @Order(63)
    @DisplayName("TS-063: Submit execution with granular step-level results returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test063_submit_execution_with_step_results() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setTestCaseId(testCaseId1);
        req.setResult(ExecResult.PASSED);

        StepResultRequest sRes = new StepResultRequest();
        sRes.setStepId(testStepId1);
        sRes.setStepNumber(1);
        sRes.setResult(ExecResult.PASSED);
        sRes.setActualResult("OTP modal opened promptly");

        req.setStepResults(List.of(sRes));

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stepResults").isArray());
    }

    @Test
    @Order(64)
    @DisplayName("TS-064: Submit execution without result fails validation (400 Bad Request)")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test064_submit_execution_missing_result_fails() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setTestCaseId(testCaseId1);

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(65)
    @DisplayName("TS-065: Submit execution without testCaseId fails validation (400 Bad Request)")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test065_submit_execution_missing_testcase_fails() throws Exception {
        SubmitExecutionRequest req = new SubmitExecutionRequest();
        req.setResult(ExecResult.PASSED);

        mockMvc.perform(post("/api/v1/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(66)
    @DisplayName("TS-066: Get execution record by valid ID returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test066_get_execution_by_id() throws Exception {
        mockMvc.perform(get("/api/v1/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(executionId.toString()));
    }

    @Test
    @Order(67)
    @DisplayName("TS-067: Get full execution history for a test case returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test067_get_execution_history_for_test_case() throws Exception {
        mockMvc.perform(get("/api/v1/executions/testcase/" + testCaseId1 + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executions").isArray())
                .andExpect(jsonPath("$.data.totalRuns").isNumber());
    }

    @Test
    @Order(68)
    @DisplayName("TS-068: List executions filtered by projectId returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test068_list_executions_by_project() throws Exception {
        mockMvc.perform(get("/api/v1/executions")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(69)
    @DisplayName("TS-069: Update existing execution record notes returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test069_update_execution_record() throws Exception {
        UpdateExecutionRequest req = new UpdateExecutionRequest();
        req.setNotes("Updated execution notes after post-mortem");

        mockMvc.perform(put("/api/v1/executions/" + executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("Updated execution notes after post-mortem"));
    }

    @Test
    @Order(70)
    @DisplayName("TS-070: Project execution summary dashboard returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test070_get_execution_summary() throws Exception {
        mockMvc.perform(get("/api/v1/executions/summary")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExecutions").isNumber());
    }

    // =========================================================================
    // MODULE 7: DEFECTS & BUG TRACKING LIFECYCLE (071 - 080)
    // =========================================================================

    @Test
    @Order(71)
    @DisplayName("TS-071: Report a new defect returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test071_create_defect_success() throws Exception {
        CreateDefectRequest req = new CreateDefectRequest();
        req.setTitle("OTP SMS delayed by over 5 minutes");
        req.setDescription("Telco gateway latency causing users to encounter session timeout");
        req.setProjectId(rootProjectId);
        req.setTestCaseId(testCaseId1);
        req.setSeverity(DefectSeverity.HIGH);
        req.setPriority(DefectPriority.P2);

        MvcResult result = mockMvc.perform(post("/api/v1/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("OTP SMS delayed by over 5 minutes"))
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        defectId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(defectId);
    }

    @Test
    @Order(72)
    @DisplayName("TS-072: Report defect without title returns 400 Bad Request")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test072_create_defect_missing_title_fails() throws Exception {
        CreateDefectRequest req = new CreateDefectRequest();
        req.setTitle("");
        req.setProjectId(rootProjectId);
        req.setSeverity(DefectSeverity.LOW);
        req.setPriority(DefectPriority.P4);

        mockMvc.perform(post("/api/v1/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(73)
    @DisplayName("TS-073: Report defect without severity returns 400 Bad Request")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test073_create_defect_missing_severity_fails() throws Exception {
        CreateDefectRequest req = new CreateDefectRequest();
        req.setTitle("Missing severity bug");
        req.setProjectId(rootProjectId);
        req.setPriority(DefectPriority.P3);

        mockMvc.perform(post("/api/v1/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(74)
    @DisplayName("TS-074: Report defect without priority returns 400 Bad Request")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test074_create_defect_missing_priority_fails() throws Exception {
        CreateDefectRequest req = new CreateDefectRequest();
        req.setTitle("Missing priority bug");
        req.setProjectId(rootProjectId);
        req.setSeverity(DefectSeverity.MEDIUM);

        mockMvc.perform(post("/api/v1/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(75)
    @DisplayName("TS-075: List defects for project returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test075_list_defects_by_project() throws Exception {
        mockMvc.perform(get("/api/v1/defects")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(defectId.toString()));
    }

    @Test
    @Order(76)
    @DisplayName("TS-076: Update defect status to IN_PROGRESS returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test076_update_defect_status_in_progress() throws Exception {
        mockMvc.perform(patch("/api/v1/defects/" + defectId + "/status")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(77)
    @DisplayName("TS-077: Update defect status to FIXED returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test077_update_defect_status_resolved() throws Exception {
        mockMvc.perform(patch("/api/v1/defects/" + defectId + "/status")
                        .param("status", "FIXED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FIXED"));
    }

    @Test
    @Order(78)
    @DisplayName("TS-078: Update defect status to CLOSED returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test078_update_defect_status_closed() throws Exception {
        mockMvc.perform(patch("/api/v1/defects/" + defectId + "/status")
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    @Order(79)
    @DisplayName("TS-079: Move defect to RETEST returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test079_update_defect_status_reopened() throws Exception {
        mockMvc.perform(patch("/api/v1/defects/" + defectId + "/status")
                        .param("status", "RETEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETEST"));
    }

    @Test
    @Order(80)
    @DisplayName("TS-080: Report critical defect with assigned developer returns 201 Created")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test080_create_critical_defect_with_assignee() throws Exception {
        CreateDefectRequest req = new CreateDefectRequest();
        req.setTitle("Data corruption during batch settlement");
        req.setDescription("Floating point rounding error in interest calculation");
        req.setProjectId(rootProjectId);
        req.setSeverity(DefectSeverity.CRITICAL);
        req.setPriority(DefectPriority.P1);
        req.setAssignedToId(adminUserId);

        mockMvc.perform(post("/api/v1/defects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.assignedTo.id").value(adminUserId.toString()));
    }

    // =========================================================================
    // MODULE 8: REQUIREMENTS, TRACEABILITY & SEARCH (081 - 088)
    // =========================================================================

    @Test
    @Order(81)
    @DisplayName("TS-081: Manager creates a business requirement returns 201 Created")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test081_create_requirement() throws Exception {
        CreateRequirementRequest req = new CreateRequirementRequest();
        req.setTitle("REQ-SEC-01: Two-Factor Authentication Compliance");
        req.setDescription("Mandatory multi-factor auth for all corporate transactions");
        req.setProjectId(rootProjectId);
        req.setType(RequirementType.FUNCTIONAL);

        MvcResult result = mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("REQ-SEC-01: Two-Factor Authentication Compliance"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        requirementId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(requirementId);
    }

    @Test
    @Order(82)
    @DisplayName("TS-082: Create requirement without title returns 400 Bad Request")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test082_create_requirement_missing_title_fails() throws Exception {
        CreateRequirementRequest req = new CreateRequirementRequest();
        req.setTitle("");
        req.setProjectId(rootProjectId);

        mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(83)
    @DisplayName("TS-083: List requirements for project returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test083_list_requirements() throws Exception {
        mockMvc.perform(get("/api/v1/requirements")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(requirementId.toString()));
    }

    @Test
    @Order(84)
    @DisplayName("TS-084: Link requirement to test case returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test084_link_requirement_to_test_case() throws Exception {
        mockMvc.perform(post("/api/v1/requirements/testcases/" + testCaseId1 + "/link/" + requirementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Linked"));
    }

    @Test
    @Order(85)
    @DisplayName("TS-085: Unlink requirement from test case returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test085_unlink_requirement_from_test_case() throws Exception {
        mockMvc.perform(delete("/api/v1/requirements/testcases/" + testCaseId1 + "/unlink/" + requirementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unlinked"));
    }

    @Test
    @Order(86)
    @DisplayName("TS-086: Global search with short query (<2 chars) returns empty array")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test086_search_short_query_returns_empty() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @Order(87)
    @DisplayName("TS-087: Global search matches existing test case returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test087_search_matching_test_case() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "login")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(88)
    @DisplayName("TS-088: Global search matches existing defect returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test088_search_matching_defect() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "SMS")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // =========================================================================
    // MODULE 9: RELEASES, QA CALLS & NOTIFICATIONS (089 - 094)
    // =========================================================================

    @Test
    @Order(89)
    @DisplayName("TS-089: Tester requests release from test case returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test089_tester_request_release() throws Exception {
        // Ensure test case is in ASSIGNED state with an active assignment
        TestCase tc1 = testCaseRepository.findById(testCaseId1).get();
        tc1.setStatus(TestStatus.ASSIGNED);
        testCaseRepository.save(tc1);

        User tester = userRepository.findByEmail("tester@example.com").get();
        if (testCaseAssignmentRepository.findByTestCase(tc1).isEmpty()) {
            testCaseAssignmentRepository.save(TestCaseAssignment.builder()
                    .testCase(tc1)
                    .assignedTo(tester)
                    .assignedBy(userRepository.findByEmail("manager@example.com").get())
                    .assignedAt(Instant.now())
                    .assignmentRole(AssignmentRole.MEMBER)
                    .build());
        }

        ReleaseTestCaseRequest req = new ReleaseTestCaseRequest();
        req.setReason(ReleaseReason.WORKLOAD_OVERLOAD);

        MvcResult result = mockMvc.perform(post("/api/v1/releases/testcases/" + testCaseId1 + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        releaseRequestId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(releaseRequestId);
    }

    @Test
    @Order(90)
    @DisplayName("TS-090: Manager lists all pending release requests returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test090_manager_list_pending_releases() throws Exception {
        mockMvc.perform(get("/api/v1/releases/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(91)
    @DisplayName("TS-091: Manager approves release request returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test091_manager_action_release_approve() throws Exception {
        ActionReleaseRequest req = new ActionReleaseRequest();
        req.setAction("APPROVED");
        req.setManagerNote("Approved. Workload adjusted.");

        mockMvc.perform(patch("/api/v1/releases/" + releaseRequestId + "/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @Order(92)
    @DisplayName("TS-092: Tester views own release requests (/api/v1/releases/mine) returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test092_tester_get_my_releases() throws Exception {
        mockMvc.perform(get("/api/v1/releases/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(93)
    @DisplayName("TS-093: Schedule a QA Call returns 201 Created")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test093_schedule_qa_call() throws Exception {
        CreateCallRequest req = new CreateCallRequest();
        req.setTitle("Sprint Defect Triage Meeting");
        req.setCallType(CallType.DEFECT_TRIAGE);
        req.setProjectId(rootProjectId);
        req.setScheduledAt(Instant.now().plusSeconds(3600));
        req.setAgenda("Review P1/P2 defect backlogs and SLA impact");
        req.setDurationMinutes(30);

        MvcResult result = mockMvc.perform(post("/api/v1/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Sprint Defect Triage Meeting"))
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        qaCallId = UUID.fromString(rootNode.path("data").path("id").asText());
        assertNotNull(qaCallId);
    }

    @Test
    @Order(94)
    @DisplayName("TS-094: List QA calls for project returns 200 OK")
    @WithMockUser(username = "tester@example.com", roles = {"TESTER"})
    void test094_list_qa_calls() throws Exception {
        mockMvc.perform(get("/api/v1/calls")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // =========================================================================
    // MODULE 10: DASHBOARDS, PRODUCTIVITY, MULTI-TENANCY & HEALTH (095 - 100)
    // =========================================================================

    @Test
    @Order(95)
    @DisplayName("TS-095: Get Manager Dashboard real-time metrics returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test095_get_manager_dashboard_metrics() throws Exception {
        mockMvc.perform(get("/api/v1/reports/manager-dashboard")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectName").exists());
    }

    @Test
    @Order(96)
    @DisplayName("TS-096: Get multi-project aggregated dashboard returns 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test096_get_all_projects_dashboard() throws Exception {
        mockMvc.perform(get("/api/v1/reports/manager-dashboard/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(97)
    @DisplayName("TS-097: Get Module Breakdown report returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test097_get_module_breakdown_report() throws Exception {
        mockMvc.perform(get("/api/v1/reports/module-breakdown")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(98)
    @DisplayName("TS-098: Team Productivity Summary report returns 200 OK")
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void test098_get_team_productivity() throws Exception {
        mockMvc.perform(get("/api/v1/productivity/team")
                        .param("projectId", rootProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testers").isArray());
    }

    @Test
    @Order(99)
    @DisplayName("TS-099: System Health status endpoint returns 200 OK with UP status")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test099_system_health_up() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @Order(100)
    @DisplayName("TS-100: System Health detailed diagnostic info returns 200 OK")
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void test100_system_health_info() throws Exception {
        mockMvc.perform(get("/api/v1/health/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.startTime").exists());
    }
}
