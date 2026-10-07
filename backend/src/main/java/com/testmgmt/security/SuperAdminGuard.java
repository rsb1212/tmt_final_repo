package com.testmgmt.security;

import com.testmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SpEL helper for {@code @PreAuthorize("@superAdminGuard.check()")}.
 *
 * <p>Returns true only when the authenticated user has
 * {@code users.is_super_admin = TRUE} and is active. Role ADMIN alone is NOT
 * enough — Team Admins (role=ADMIN, is_super_admin=FALSE) are rejected.
 *
 * <p>Used to restrict organisation-wide operations (Tenants page, creating /
 * editing / deleting Teams, adding / removing team members) to the Super Admin.
 */
@Component("superAdminGuard")
@RequiredArgsConstructor
public class SuperAdminGuard {

    private final UserRepository userRepository;

    public boolean check() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return false;
        }
        return userRepository.findByEmail(auth.getName())
                .map(u -> Boolean.TRUE.equals(u.getIsSuperAdmin())
                        && !Boolean.FALSE.equals(u.getActive()))
                .orElse(false);
    }
}
