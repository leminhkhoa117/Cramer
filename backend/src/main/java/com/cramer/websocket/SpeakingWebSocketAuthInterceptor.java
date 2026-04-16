package com.cramer.websocket;

import com.cramer.util.JwtUtil;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class SpeakingWebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(
        SpeakingWebSocketAuthInterceptor.class
    );

    private final JwtUtil jwtUtil;

    public SpeakingWebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(
        @NonNull ServerHttpRequest request,
        @NonNull ServerHttpResponse response,
        @NonNull WebSocketHandler wsHandler,
        @NonNull Map<String, Object> attributes
    ) {
        try {
            SpeakingWebSocketAuthSupport.ExtractedToken extractedToken =
                SpeakingWebSocketAuthSupport.extractToken(
                    request.getHeaders(),
                    request.getURI()
                );
            UUID userId = SpeakingWebSocketAuthSupport.requireAuthenticatedUserId(
                jwtUtil,
                extractedToken != null ? extractedToken.value() : null
            );

            attributes.put(
                SpeakingWebSocketAuthSupport.AUTHENTICATED_USER_ID_ATTR,
                userId.toString()
            );
            if (extractedToken != null) {
                attributes.put(
                    SpeakingWebSocketAuthSupport.AUTH_TOKEN_SOURCE_ATTR,
                    extractedToken.source()
                );
            }
            return true;
        } catch (IllegalArgumentException ex) {
            log.debug(
                "Rejecting Speaking WebSocket handshake for {}: {}",
                request.getURI(),
                ex.getMessage()
            );
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response
                .getHeaders()
                .set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            return false;
        }
    }

    @Override
    public void afterHandshake(
        @NonNull ServerHttpRequest request,
        @NonNull ServerHttpResponse response,
        @NonNull WebSocketHandler wsHandler,
        @Nullable Exception exception
    ) {}
}