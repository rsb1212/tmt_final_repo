package com.testmgmt.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.testmgmt.dto.response.ResponseDTOs.AuthResponse;
import com.testmgmt.dto.response.ResponseDTOs.TenantResponse;
import com.testmgmt.entity.Tenant;
import com.testmgmt.entity.User;
import com.testmgmt.enums.UserRole;
import com.testmgmt.exception.BadRequestException;
import com.testmgmt.repository.TenantRepository;
import com.testmgmt.repository.UserRepository;
import com.testmgmt.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Value("${app.keycloak.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${app.keycloak.issuer-uri:}")
    private String issuerUri;

    private volatile JwtDecoder jwtDecoder;

    @Transactional
    public AuthResponse keycloakLogin(String keycloakToken) {

        log.info("========== IDEM LOGIN START ==========");

        if (keycloakToken == null || keycloakToken.isBlank()) {
            throw new BadRequestException("Keycloak token is required");
        }

        String raw = keycloakToken.startsWith("Bearer ")
                ? keycloakToken.substring(7)
                : keycloakToken;

        Jwt jwt = decode(raw);

        log.info("Token validated successfully");
        log.info("Subject: {}", jwt.getSubject());
        log.info("Email: {}", jwt.getClaimAsString("email"));

        String email = firstNonBlank(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"));

        if (email == null) {
            throw new BadRequestException(
                    "Keycloak token does not contain email or username");
        }

        String fullName = firstNonBlank(
                jwt.getClaimAsString("name"),
                email);

        UserRole role = extractRole(jwt);

        User user = userRepository.findByEmail(email)
                .map(existing -> syncUser(existing, fullName, role))
                .orElseGet(() -> provisionUser(email, fullName, role));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String tenantId =
                user.getTenantId() != null
                        ? user.getTenantId().toString()
                        : null;

        String appToken =
                jwtUtil.generateToken(userDetails, tenantId);

        log.info("========== IDEM LOGIN SUCCESS ==========");

        return AuthResponse.builder()
                .token(appToken)
                .tokenType("Bearer")
                .user(AuthService.toUserResponse(user))
                .tenant(loadTenant(user.getTenantId()))
                .build();
    }

    private Jwt decode(String raw) {
        try {
            return decoder().decode(raw);
        } catch (Exception ex) {
            log.error("Token validation failed", ex);
            throw new BadRequestException(
                    "Invalid or expired IDEM/Keycloak token");
        }
    }

    private JwtDecoder decoder() {

        JwtDecoder local = jwtDecoder;

        if (local == null) {
            synchronized (this) {

                local = jwtDecoder;

                if (local == null) {

                    if (jwkSetUri == null || jwkSetUri.isBlank()) {
                        throw new BadRequestException(
                                "Missing app.keycloak.jwk-set-uri configuration");
                    }

                    NimbusJwtDecoder decoder =
                            NimbusJwtDecoder
                                    .withJwkSetUri(jwkSetUri)
                                    .build();

                    if (issuerUri != null && !issuerUri.isBlank()) {

                        OAuth2TokenValidator<Jwt> validator =
                                new DelegatingOAuth2TokenValidator<>(
                                        JwtValidators.createDefaultWithIssuer(
                                                issuerUri));

                        decoder.setJwtValidator(validator);
                    }

                    local = decoder;
                    jwtDecoder = local;
                }
            }
        }

        return local;
    }

    @SuppressWarnings("unchecked")
    private UserRole extractRole(Jwt jwt) {

        Object realmAccess = jwt.getClaim("realm_access");

        if (realmAccess instanceof Map<?, ?> map) {

            Object rolesObj = map.get("roles");

            if (rolesObj instanceof List<?> roles) {

                for (Object role : (List<Object>) roles) {

                    UserRole mapped =
                            mapRole(String.valueOf(role));

                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }

        return UserRole.TESTER;
    }

    private UserRole mapRole(String role) {

        if (role == null) {
            return null;
        }

        String normalized =
                role.trim()
                        .toUpperCase()
                        .replace("ROLE_", "");

        return switch (normalized) {
            case "ADMIN" -> UserRole.ADMIN;
            case "MANAGER", "LEAD" -> UserRole.MANAGER;
            case "SME" -> UserRole.SME;
            case "TESTER", "USER" -> UserRole.TESTER;
            case "VIEWER" -> UserRole.VIEWER;
            default -> null;
        };
    }

    private User provisionUser(
            String email,
            String fullName,
            UserRole role) {

        User user = User.builder()
                .username(uniqueUsername(email))
                .email(email)
                .passwordHash(
                        passwordEncoder.encode(
                                UUID.randomUUID().toString()))
                .fullName(fullName)
                .role(role)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    private User syncUser(
            User existing,
            String fullName,
            UserRole role) {

        boolean dirty = false;

        if (Boolean.FALSE.equals(existing.getActive())) {
            existing.setActive(true);
            dirty = true;
        }

        if (fullName != null &&
                !fullName.equals(existing.getFullName())) {

            existing.setFullName(fullName);
            dirty = true;
        }

        if (role != null &&
                role != existing.getRole()) {

            existing.setRole(role);
            dirty = true;
        }

        return dirty
                ? userRepository.save(existing)
                : existing;
    }

    private String uniqueUsername(String email) {

        String base =
                email.contains("@")
                        ? email.substring(0, email.indexOf("@"))
                        : email;

        if (base.length() > 45) {
            base = base.substring(0, 45);
        }

        String candidate = base;
        int suffix = 1;

        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }

        return candidate;
    }

    private TenantResponse loadTenant(UUID tenantId) {

        if (tenantId == null) {
            return null;
        }

        Tenant tenant =
                tenantRepository.findById(tenantId)
                        .orElse(null);

        if (tenant == null) {
            return null;
        }

        return TenantResponse.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .description(tenant.getDescription())
                .active(tenant.getActive())
                .build();
    }

    private static String firstNonBlank(String... values) {

        for (String value : values) {

            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }
}