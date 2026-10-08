package com.testmgmt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testmgmt.dto.request.AuthDTOs.LoginRequest;
import com.testmgmt.dto.request.WorkflowDTOs.CreateProjectRequest;
import com.testmgmt.entity.Team;
import com.testmgmt.entity.Tenant;
import com.testmgmt.entity.User;
import com.testmgmt.repository.TeamRepository;
import com.testmgmt.repository.TenantRepository;
import com.testmgmt.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class TeamsAndProjectsSeedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private static final String DEFAULT_PASSWORD = "Password@123";

    private String adminToken;
    private String managerToken;
    private String smeToken;
    private String testerToken;

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @Test
    @Order(1)
    @DisplayName("Verify Database contains 3 seeded Teams with proper metadata")
    void testTeamsExistInDatabase() {
        Tenant tenant = tenantRepository.findByCode("DEFAULT").orElseThrow();

        Optional<Team> coreTeam = teamRepository.findByTenantIdAndCode(tenant.getId(), "QA_CORE");
        assertTrue(coreTeam.isPresent(), "QA_CORE team must exist");
        assertEquals("Core Banking QA Team", coreTeam.get().getName());
        assertEquals("Core Banking", coreTeam.get().getDepartment());
        assertEquals("QA", coreTeam.get().getTeamType());

        Optional<Team> digitalTeam = teamRepository.findByTenantIdAndCode(tenant.getId(), "QA_DIGITAL");
        assertTrue(digitalTeam.isPresent(), "QA_DIGITAL team must exist");
        assertEquals("Digital Payments QA Team", digitalTeam.get().getName());
        assertEquals("Digital Channels", digitalTeam.get().getDepartment());
        assertEquals("QA", digitalTeam.get().getTeamType());

        Optional<Team> integrationTeam = teamRepository.findByTenantIdAndCode(tenant.getId(), "QA_INTEGRATION");
        assertTrue(integrationTeam.isPresent(), "QA_INTEGRATION team must exist");
        assertEquals("Enterprise Integration QA Team", integrationTeam.get().getName());
        assertEquals("Integration & Middleware", integrationTeam.get().getDepartment());
        assertEquals("QA", integrationTeam.get().getTeamType());
    }

    @Test
    @Order(2)
    @DisplayName("Test Member Authentication and Role Verification (ADMIN, MANAGER, SME, TESTER)")
    void testMembersLoginAndRoles() throws Exception {
        // 1. ADMIN
        adminToken = loginAndGetToken("admin@testgenii.com", DEFAULT_PASSWORD);
        assertNotNull(adminToken, "Admin JWT token must not be null");

        // 2. MANAGER
        managerToken = loginAndGetToken("manager@testgenii.com", DEFAULT_PASSWORD);
        assertNotNull(managerToken, "Manager JWT token must not be null");

        // 3. SME
        smeToken = loginAndGetToken("sme@testgenii.com", DEFAULT_PASSWORD);
        assertNotNull(smeToken, "SME JWT token must not be null");

        // 4. TESTER
        testerToken = loginAndGetToken("tester@testgenii.com", DEFAULT_PASSWORD);
        assertNotNull(testerToken, "Tester JWT token must not be null");

        // Verify entity roles and team assignments directly
        User admin = userRepository.findByEmail("admin@testgenii.com").orElseThrow();
        assertEquals("ADMIN", admin.getRole().name());
        assertEquals("Core Banking QA Team", admin.getTeam());

        User manager = userRepository.findByEmail("manager@testgenii.com").orElseThrow();
        assertEquals("MANAGER", manager.getRole().name());
        assertEquals("Core Banking QA Team", manager.getTeam());

        User sme = userRepository.findByEmail("sme@testgenii.com").orElseThrow();
        assertEquals("SME", sme.getRole().name());
        assertEquals("Digital Payments QA Team", sme.getTeam());

        User tester = userRepository.findByEmail("tester@testgenii.com").orElseThrow();
        assertEquals("TESTER", tester.getRole().name());
        assertEquals("Enterprise Integration QA Team", tester.getTeam());
    }

    @Test
    @Order(3)
    @DisplayName("Verify Team API endpoint GET /api/v1/teams returns all 3 seeded teams")
    void testGetTeamsApiEndpoint() throws Exception {
        assertNotNull(managerToken, "Manager token should be initialized");

        mockMvc.perform(get("/api/v1/teams")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.code == 'QA_CORE')].name").value("Core Banking QA Team"))
                .andExpect(jsonPath("$.data[?(@.code == 'QA_DIGITAL')].name").value("Digital Payments QA Team"))
                .andExpect(jsonPath("$.data[?(@.code == 'QA_INTEGRATION')].name").value("Enterprise Integration QA Team"));
    }

    @Test
    @Order(4)
    @DisplayName("Verify Projects API endpoint GET /api/v1/projects returns the seeded projects")
    void testGetProjectsApiEndpoint() throws Exception {
        assertNotNull(adminToken, "Admin token should be initialized");

        MvcResult result = mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("Core Banking Modernization"), "Must contain Core Banking Modernization project");
        assertTrue(responseBody.contains("Mobile Banking & UPI 2.0"), "Must contain Mobile Banking & UPI 2.0 project");
        assertTrue(responseBody.contains("Open Banking & SWIFT Gateway"), "Must contain Open Banking & SWIFT Gateway project");
    }

    @Test
    @Order(5)
    @DisplayName("Verify Role-Based Access Control on Project Management (Manager vs Tester)")
    void testRbacOnProjects() throws Exception {
        assertNotNull(managerToken);
        assertNotNull(testerToken);

        // 1. Manager is authorized to create a sub-project
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Core Ledger Microservice");
        req.setDescription("Sub-project managed by Core QA Manager");

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Core Ledger Microservice"));

        // 2. Tester is NOT authorized to create a project (403 Forbidden)
        CreateProjectRequest unauthorizedReq = new CreateProjectRequest();
        unauthorizedReq.setName("Unauthorized Project Creation");
        unauthorizedReq.setDescription("Should be rejected for Tester role");

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + testerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedReq)))
                .andExpect(status().isForbidden());

        // 3. Tester is authorized to read projects
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("Verify SME and Tester can access their profile and modules")
    void testSmeAndTesterDataFlow() throws Exception {
        assertNotNull(smeToken);
        assertNotNull(testerToken);

        // SME can fetch profile/current user
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + smeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("sme@testgenii.com"))
                .andExpect(jsonPath("$.data.role").value("SME"));

        // Tester can fetch profile/current user
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + testerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("tester@testgenii.com"))
                .andExpect(jsonPath("$.data.role").value("TESTER"));
    }
}
