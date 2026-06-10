package com.cramer.controller;

import com.cramer.config.TestSecurityConfig;
import com.cramer.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base class for controller tests.
 * Provides common utilities for MockMvc testing with JWT authentication.
 *
 * @author Cramer Test Team
 * @since 2026-01-19
 */
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
public abstract class BaseControllerTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // Default test user ID
    protected static final UUID DEFAULT_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    protected static final String DEFAULT_USER_ID_STRING = "550e8400-e29b-41d4-a716-446655440000";

    /**
     * Perform GET request with JWT authentication.
     */
    protected ResultActions performGet(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .with(jwt().jwt(jwt -> jwt
                        .subject(DEFAULT_USER_ID_STRING)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /**
     * Perform GET request with specific user ID.
     */
    protected ResultActions performGetAs(UUID userId, String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars)
                .with(jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /**
     * Perform GET request without authentication.
     */
    protected ResultActions performGetNoAuth(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(get(url, uriVars));
    }

    /**
     * Perform POST request with JWT authentication and JSON body.
     */
    protected ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(jwt().jwt(jwt -> jwt
                        .subject(DEFAULT_USER_ID_STRING)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Perform POST request with specific user ID.
     */
    protected ResultActions performPostAs(UUID userId, String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Perform POST request without authentication.
     */
    protected ResultActions performPostNoAuth(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Perform PUT request with JWT authentication.
     */
    protected ResultActions performPut(String url, Object body, Object... uriVars) throws Exception {
        return mockMvc.perform(put(url, uriVars)
                .with(jwt().jwt(jwt -> jwt
                        .subject(DEFAULT_USER_ID_STRING)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Perform PUT request with specific user ID.
     */
    protected ResultActions performPutAs(UUID userId, String url, Object body, Object... uriVars) throws Exception {
        return mockMvc.perform(put(url, uriVars)
                .with(jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Perform DELETE request with JWT authentication.
     */
    protected ResultActions performDelete(String url, Object... uriVars) throws Exception {
        return mockMvc.perform(delete(url, uriVars)
                .with(jwt().jwt(jwt -> jwt
                        .subject(DEFAULT_USER_ID_STRING)
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /**
     * Perform DELETE request with specific user ID.
     */
    protected ResultActions performDeleteAs(UUID userId, String url, Object... uriVars) throws Exception {
        return mockMvc.perform(delete(url, uriVars)
                .with(jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("aud", "authenticated"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /**
     * Add query parameters to a request.
     */
    protected MockHttpServletRequestBuilder withParams(MockHttpServletRequestBuilder builder, String... params) {
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        return builder;
    }

    /**
     * Convert object to JSON string.
     */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Generate a random UUID for testing.
     */
    protected UUID randomUUID() {
        return UUID.randomUUID();
    }
}