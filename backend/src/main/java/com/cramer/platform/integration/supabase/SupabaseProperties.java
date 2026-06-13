package com.cramer.platform.integration.supabase;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Supabase connection settings (SPEC-18 §4, SPEC-04 §6). Bound from {@code supabase.*} /
 * {@code SUPABASE_*} environment variables.
 *
 * @param url            project URL (e.g. {@code https://xyz.supabase.co})
 * @param anonKey        anon public key (client-facing; rarely used server-side)
 * @param serviceRoleKey service-role key (admin operations — keep secret)
 * @param jwtSecret      HS256 secret used to verify access tokens (SPEC-18 §1)
 * @param insecureTls    dev-only: trust all TLS certs (self-hosted/self-signed). Never in prod.
 */
@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(
        String url,
        String anonKey,
        String serviceRoleKey,
        String jwtSecret,
        boolean insecureTls) {

    public String normalizedUrl() {
        return url == null ? "" : url.trim().replaceAll("/+$", "");
    }
}
