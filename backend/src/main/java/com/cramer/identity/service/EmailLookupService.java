package com.cramer.identity.service;

import com.cramer.platform.integration.supabase.SupabaseAdminClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

/**
 * Email existence check (SPEC-10 §2.1) over the Supabase Auth Admin API. Paginates the user
 * list and matches case-insensitively.
 *
 * <p><strong>Fix:</strong> on upstream failure the underlying client throws
 * {@link com.cramer.platform.error.UpstreamServiceException} (→ 503); this service propagates it
 * and never fabricates {@code exists:false} (which previously risked duplicate-signup confusion).
 */
@Service
public class EmailLookupService {

    private static final int PER_PAGE = 50;
    private static final int MAX_PAGES = 200; // safety bound (10k users)

    private final SupabaseAdminClient adminClient;

    public EmailLookupService(SupabaseAdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String target = email.trim();
        for (int page = 1; page <= MAX_PAGES; page++) {
            JsonNode body = adminClient.listUsersPage(page, PER_PAGE);
            JsonNode users = body.path("users");
            if (!users.isArray() || users.isEmpty()) {
                return false;
            }
            for (JsonNode user : users) {
                if (target.equalsIgnoreCase(user.path("email").asText(null))) {
                    return true;
                }
            }
            if (users.size() < PER_PAGE) {
                return false; // last page reached
            }
        }
        return false;
    }
}
