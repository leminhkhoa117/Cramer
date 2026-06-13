package com.cramer.platform.integration.supabase;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.UpstreamServiceException;
import com.cramer.platform.integration.InsecureHttpClients;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin client for the Supabase Auth Admin API (service-role key). Used by identity for email
 * existence checks (SPEC-18 §4). Contains no business logic — pagination and email matching
 * live in {@code EmailLookupService} (SPEC-10 §2.1).
 *
 * <p>On any transport or non-2xx response it throws {@link UpstreamServiceException} (→ 503),
 * so callers never fabricate a misleading {@code exists:false} on upstream failure.
 */
@Component
public class SupabaseAdminClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAdminClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String serviceRoleKey;
    private final HttpClient httpClient;

    public SupabaseAdminClient(SupabaseProperties props) {
        this.baseUrl = props.normalizedUrl();
        this.serviceRoleKey = props.serviceRoleKey() == null ? "" : props.serviceRoleKey().trim();
        this.httpClient = props.insecureTls()
                ? InsecureHttpClients.trustAll(TIMEOUT)
                : InsecureHttpClients.secure(TIMEOUT);
        if (props.insecureTls()) {
            log.warn("Supabase insecure TLS is ENABLED — development only. Never use in production.");
        }
    }

    /**
     * Fetch one page of auth users from {@code GET /auth/v1/admin/users}.
     *
     * @param page    1-based page number
     * @param perPage page size
     * @return parsed JSON body (shape {@code { "users": [ ... ] }})
     * @throws UpstreamServiceException on transport failure or a non-2xx response
     */
    public JsonNode listUsersPage(int page, int perPage) {
        String url = baseUrl + "/auth/v1/admin/users?page=" + page + "&per_page=" + perPage;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return Json.readTree(resp.body());
            }
            log.error("Supabase admin/users failed: status={}", resp.statusCode());
            throw new UpstreamServiceException("Supabase auth admin returned HTTP " + resp.statusCode());
        } catch (java.io.IOException e) {
            throw new UpstreamServiceException("Supabase auth admin request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Supabase auth admin request interrupted", e);
        }
    }
}
