package com.cramer.platform.security;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.web.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP security (SPEC-04 §1, SPEC-18 §1): stateless, CSRF off, CORS on, OAuth2 resource-server
 * JWT verification. Route rules follow SPEC-04 §1.1; {@code /api/admin/**} additionally requires
 * {@code profiles.is_admin} via {@link AdminAuthorizationService}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminAuthorizationService adminAuthorization;

    public SecurityConfig(AdminAuthorizationService adminAuthorization) {
        this.adminAuthorization = adminAuthorization;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/health", "/api/health/**",
                                "/api/payments/webhook", "/api/payments/config-status", "/api/payments/lua-packs",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
                                "/error", "/ws/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**").access((authentication, context) ->
                                new AuthorizationDecision(adminAuthorization.isAdmin(authentication.get())))
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, req, 401, "Unauthorized", "Authentication required"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, req, 403, "Forbidden", "Access denied")));
        return http.build();
    }

    /** Writes an {@link ApiError} body for filter-chain auth failures (consistent with §2). */
    private static void writeError(HttpServletResponse res, HttpServletRequest req,
                                   int status, String error, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError body = ApiError.of(status, error, message, req.getRequestURI());
        res.getWriter().write(Json.toJson(body));
    }
}
