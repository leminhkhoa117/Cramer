package com.cramer.platform.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression for the boot-smoke finding: an unknown path must map to 404 (not 500). Spring 7's
 * {@link NoResourceFoundException} implements {@code ErrorResponse} (it extends
 * {@code ServletException}, not {@code ErrorResponseException}); the catch-all must honor its
 * carried status instead of forcing 500 (SPEC-04 §2.2).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unknownPathMapsToNotFound() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health/nope");
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/health/nope", null);

        ResponseEntity<ApiError> response = handler.handleUnexpected(ex, req);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().path()).isEqualTo("/api/health/nope");
        // 4xx must not leak an exceptionType (that's a 500-only field)
        assertThat(response.getBody().exceptionType()).isNull();
    }

    @Test
    void responseStatusExceptionStatusIsHonored() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/x");
        ErrorResponseException ex = new ErrorResponseException(HttpStatus.METHOD_NOT_ALLOWED);

        ResponseEntity<ApiError> response = handler.handleUnexpected(ex, req);

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody().error()).isEqualTo("Method Not Allowed");
    }

    @Test
    void genuineServerErrorStillMapsTo500() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        ResponseEntity<ApiError> response = handler.handleUnexpected(new RuntimeException("boom"), req);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().exceptionType()).isEqualTo("RuntimeException");
    }
}
