package com.testmgmt;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testmgmt.dto.request.AuthDTOs.RegisterRequest;

@SuppressWarnings("null")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_flow() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("testuser@example.com");
        registerRequest.setPassword("Test@1234");
        registerRequest.setFullName("Test User");

        // Register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("TESTER"));
    }

    @Test
    void keycloak_login_with_missing_token_returns_400() throws Exception {
        // Local username/password login has been removed in favour of IDEM/Keycloak
        // SSO. Authorization is DB-driven and users are never auto-provisioned as
        // TESTER. A keycloak-login request without a token must be rejected.
        mockMvc.perform(post("/api/v1/auth/keycloak-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_with_invalid_data_returns_400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("ab");        // too short (min 3)
        req.setEmail("not-an-email");
        req.setPassword("short");     // too short (min 8)

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
