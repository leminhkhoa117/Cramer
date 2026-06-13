package com.cramer.platform.security;

import com.cramer.platform.integration.supabase.SupabaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Builds the resource-server {@link JwtDecoder} from the Supabase HS256 secret (SPEC-04 §1.2,
 * SPEC-18 §1). Validates signature + expiry (Nimbus default timestamp validator). The custom
 * {@code JwtAuthFilter} is gone — token verification is now standard Spring Security.
 */
@Configuration
public class SupabaseJwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(SupabaseProperties props) {
        String secret = props.jwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Supabase JWT secret is not configured (set SUPABASE_JWT_SECRET / supabase.jwt-secret).");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "Supabase JWT secret must be at least 32 bytes for HS256 (got " + keyBytes.length + ").");
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
