package com.cramer.platform.security;

import com.cramer.platform.error.OperationNotAllowedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authenticated user's UUID from the security context (SPEC-04 §1.2). The
 * principal name is the Supabase {@code sub} claim. Controllers obtain the user id from here —
 * never from request bodies or an {@code X-User-Id} header (the spoofable-audit defect is gone).
 */
@Component
public class CurrentUser {

    /**
     * @return the authenticated user id
     * @throws OperationNotAllowedException if the request is not authenticated (defensive; on
     *         protected routes the resource-server filter already guarantees authentication)
     */
    public UUID requireUserId() {
        return userId().orElseThrow(() -> new OperationNotAllowedException("Authentication required"));
    }

    /** @return the authenticated user id if present and a valid UUID, else empty. */
    public Optional<UUID> userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        String name = auth.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
