package com.cramer.platform.security;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Admin authorization check (SPEC-04 §1.3, SPEC-18 §1): is {@code profiles.is_admin = true}?
 *
 * <p>Lives in the platform security kernel and reads the flag via a narrow {@link JdbcTemplate}
 * query rather than depending on the {@code identity} module's repository — {@code platform}
 * must not depend on any business module (SPEC-01 §3, rule 1). Wired into the security chain as
 * an {@code AuthorizationManager} for {@code /api/admin/**}; admin identity is always derived
 * from the verified token + DB, never a header.
 */
@Service
public class AdminAuthorizationService {

    private final JdbcTemplate jdbcTemplate;

    public AdminAuthorizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Resolves the principal UUID from the authentication and checks the admin flag. */
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        try {
            return isAdmin(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException e) {
            return false; // anonymous / non-UUID principal
        }
    }

    /** @return true iff a profile row exists for {@code userId} with {@code is_admin = true}. */
    public boolean isAdmin(UUID userId) {
        if (userId == null) {
            return false;
        }
        try {
            Boolean isAdmin = jdbcTemplate.queryForObject(
                    "SELECT is_admin FROM profiles WHERE id = ?", Boolean.class, userId);
            return Boolean.TRUE.equals(isAdmin);
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}
