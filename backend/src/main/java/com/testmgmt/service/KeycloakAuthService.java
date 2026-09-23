package com.testmgmt.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
import com.testmgmt.exception.UnauthorizedException;
import com.testmgmt.repository.TenantRepository;
import com.testmgmt.repository.UserRepository;
import com.testmgmt.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Token-based IDEM / Keycloak login.
 *
 * <p>Authorization is controlled entirely by the application database. After the
 * IDEM/Keycloak token is validated, the user is looked up by email. If the user
 * does not exist in the Users table, access is denied. Users are NEVER
 * auto-provisioned and NEVER receive a default TESTER role. The role that ends
 * up in the app session JWT always comes from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
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

        log.info("User authenticated from IDEM: email={}", email);

        // Authorization is controlled by the application database. The user must
        // already exist in the Users table. We DO NOT auto-provision users and
        // DO NOT assign a default TESTER role.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("IDEM user not found in database - denying access: email={}", email);
                    return new UnauthorizedException(
                            "User is not registered in the application. Please contact your administrator.");
                });

        if (Boolean.FALSE.equals(user.getActive())) {
            log.warn("IDEM user is disabled - denying access: email={}", email);
            throw new UnauthorizedException(
                    "Your account is disabled. Please contact your administrator.");
        }

        log.info("User found in database: email={}, role assigned from database={}",
                email, user.getRole());

        // Keep the display name fresh, but the role ALWAYS comes from the DB.
        user = syncUser(user, fullName);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String tenantId =
                user.getTenantId() != null
                        ? user.getTenantId().toString()
                        : null;

        String appToken =
                jwtUtil.generateToken(userDetails, tenantId);

        log.info("========== IDEM LOGIN SUCCESS ========== email={} role={} redirect={}",
                email, user.getRole(), dashboardFor(user.getRole()));

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

    /**
     * Keep profile details (name) fresh on each login WITHOUT ever changing the
     * role. The role is always taken from the database.
     */
    private User syncUser(User existing, String fullName) {

        boolean dirty = false;

        if (fullName != null && !fullName.equals(existing.getFullName())) {
            existing.setFullName(fullName);
            dirty = true;
        }

        return dirty ? userRepository.save(existing) : existing;
    }

    /** Human-friendly dashboard destination used only for logging. */
    private String dashboardFor(UserRole role) {
        if (role == null) {
            return "/login";
        }
        return switch (role) {
            case ADMIN -> "/admin-dashboard";
            case MANAGER -> "/manager-dashboard";
            default -> "/dashboard";
        };
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