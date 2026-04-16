package com.cramer.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cramer.util.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

@DisplayName("SpeakingWebSocketAuthInterceptor Unit Tests")
@SuppressWarnings("null")
class SpeakingWebSocketAuthInterceptorTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final UUID USER_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000123"
    );

    @Test
    @DisplayName(
        "beforeHandshake should accept a valid query token and store authenticated user id"
    )
    void beforeHandshake_validQueryToken_allowsHandshake() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.extractUserId(VALID_TOKEN)).thenReturn(USER_ID.toString());

        SpeakingWebSocketAuthInterceptor interceptor =
            new SpeakingWebSocketAuthInterceptor(jwtUtil);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
            "GET",
            "/ws/speaking/42"
        );
        servletRequest.setQueryString("token=" + VALID_TOKEN);
        servletRequest.setRequestURI("/ws/speaking/42");
        ServletServerHttpRequest request = new ServletServerHttpRequest(
            servletRequest
        );
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(
            servletResponse
        );
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
            request,
            response,
            mock(WebSocketHandler.class),
            attributes
        );

        assertThat(allowed).isTrue();
        assertThat(attributes)
            .containsEntry(
                SpeakingWebSocketAuthSupport.AUTHENTICATED_USER_ID_ATTR,
                USER_ID.toString()
            )
            .containsEntry(
                SpeakingWebSocketAuthSupport.AUTH_TOKEN_SOURCE_ATTR,
                SpeakingWebSocketAuthSupport.AUTH_TOKEN_SOURCE_QUERY_PARAMETER
            );
        verify(jwtUtil).validateToken(VALID_TOKEN);
        verify(jwtUtil).extractUserId(VALID_TOKEN);
    }

    @Test
    @DisplayName(
        "beforeHandshake should reject missing token before the WebSocket upgrades"
    )
    void beforeHandshake_missingToken_rejectsHandshake() {
        JwtUtil jwtUtil = mock(JwtUtil.class);

        SpeakingWebSocketAuthInterceptor interceptor =
            new SpeakingWebSocketAuthInterceptor(jwtUtil);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
            "GET",
            "/ws/speaking/42"
        );
        servletRequest.setRequestURI("/ws/speaking/42");
        ServletServerHttpRequest request = new ServletServerHttpRequest(
            servletRequest
        );
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(
            servletResponse
        );

        boolean allowed = interceptor.beforeHandshake(
            request,
            response,
            mock(WebSocketHandler.class),
            new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(
            HttpStatus.UNAUTHORIZED.value()
        );
    }

    @Test
    @DisplayName(
        "beforeHandshake should reject invalid tokens with unauthorized response"
    )
    void beforeHandshake_invalidToken_rejectsHandshake() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(false);

        SpeakingWebSocketAuthInterceptor interceptor =
            new SpeakingWebSocketAuthInterceptor(jwtUtil);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
            "GET",
            "/ws/speaking/42"
        );
        servletRequest.addHeader(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + VALID_TOKEN
        );
        servletRequest.setRequestURI("/ws/speaking/42");
        ServletServerHttpRequest request = new ServletServerHttpRequest(
            servletRequest
        );
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(
            servletResponse
        );

        boolean allowed = interceptor.beforeHandshake(
            request,
            response,
            mock(WebSocketHandler.class),
            new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(
            HttpStatus.UNAUTHORIZED.value()
        );
        verify(jwtUtil).validateToken(VALID_TOKEN);
    }
}