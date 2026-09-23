package com.testmgmt.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.testmgmt.dto.request.AuthDTOs.ChangePasswordRequest;
import com.testmgmt.dto.request.AuthDTOs.RegisterRequest;
import com.testmgmt.dto.response.ResponseDTOs.ApiResponse;
import com.testmgmt.dto.response.ResponseDTOs.AuthResponse;
import com.testmgmt.service.AuthService;
import com.testmgmt.service.KeycloakAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;
    private final KeycloakAuthService keycloakAuthService;

    /**
     * Keycloak Login
     */
    @PostMapping("/keycloak-login")
    public ResponseEntity<ApiResponse<AuthResponse>> keycloakLogin(
            @RequestBody Map<String, String> body) {

        log.info("KEYCLOAK LOGIN API CALLED");

        String token = body.get("token");

        AuthResponse response =
                keycloakAuthService.keycloakLogin(token);

        return ResponseEntity.ok(
                ApiResponse.success(response));
    }



    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User registered successfully",
                        authService.register(request)));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change own password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(
                userDetails.getUsername(),
                request);

        return ResponseEntity.ok(
                ApiResponse.ok("Password changed successfully"));
    }
}