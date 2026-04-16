package com.cramer.websocket;

import com.cramer.util.JwtUtil;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

final class SpeakingWebSocketAuthSupport {

    static final String AUTHENTICATED_USER_ID_ATTR =
        "speaking.authenticatedUserId";
    static final String AUTH_TOKEN_SOURCE_ATTR = "speaking.authTokenSource";
    static final String AUTH_TOKEN_SOURCE_AUTHORIZATION_HEADER =
        "authorization_header";
    static final String AUTH_TOKEN_SOURCE_QUERY_PARAMETER =
        "query_parameter";

    private SpeakingWebSocketAuthSupport() {}

    @Nullable
    static ExtractedToken extractToken(HttpHeaders headers, @Nullable URI uri) {
        String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (
            authorizationHeader != null &&
            authorizationHeader.startsWith("Bearer ")
        ) {
            String token = authorizationHeader.substring(7).trim();
            if (!token.isBlank()) {
                return new ExtractedToken(
                    token,
                    AUTH_TOKEN_SOURCE_AUTHORIZATION_HEADER
                );
            }
        }

        if (uri == null) {
            return null;
        }

        var queryParams = UriComponentsBuilder.fromUri(uri)
            .build()
            .getQueryParams();
        String accessToken = queryParams.getFirst("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return new ExtractedToken(
                accessToken.trim(),
                AUTH_TOKEN_SOURCE_QUERY_PARAMETER
            );
        }

        String legacyToken = queryParams.getFirst("token");
        if (legacyToken != null && !legacyToken.isBlank()) {
            return new ExtractedToken(
                legacyToken.trim(),
                AUTH_TOKEN_SOURCE_QUERY_PARAMETER
            );
        }

        return null;
    }

    static UUID requireAuthenticatedUserId(
        JwtUtil jwtUtil,
        @Nullable String token
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                "Missing JWT. Provide Authorization header or access_token/token query parameter."
            );
        }

        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired JWT.");
        }

        String subject = jwtUtil.extractUserId(token);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing.");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "JWT subject is not a valid UUID."
            );
        }
    }

    @Nullable
    static UUID getAuthenticatedUserId(@Nullable Map<String, Object> attributes) {
        if (attributes == null) {
            return null;
        }

        Object value = attributes.get(AUTHENTICATED_USER_ID_ATTR);
        if (value instanceof UUID userId) {
            return userId;
        }
        if (value instanceof String userId) {
            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                    "Handshake user id is not a valid UUID.",
                    ex
                );
            }
        }

        return null;
    }

    record ExtractedToken(String value, String source) {}
}