package com.cramer.platform.web;

import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.PayloadTooLargeException;
import com.cramer.platform.error.QuotaExceededException;
import com.cramer.platform.error.RateLimitExceededException;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import com.cramer.platform.error.ResourceNotFoundException;
import com.cramer.platform.error.UpstreamServiceException;
import com.cramer.platform.integration.openrouter.OpenRouterException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single translation point from exceptions to HTTP responses (SPEC-04 §2.2, SPEC-18 §3). Every
 * error response is a {@link ApiError}; stack traces and internal details never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 404 ----
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // ---- 409 ----
    @ExceptionHandler({ResourceAlreadyExistsException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    // ---- 403 ----
    @ExceptionHandler({OperationNotAllowedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleForbidden(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    // ---- 400 (bad input) ----
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest req) {
        String message = (ex instanceof HttpMessageNotReadableException)
                ? "Malformed or unreadable request body"
                : ex.getMessage();
        return respond(HttpStatus.BAD_REQUEST, message, req);
    }

    // ---- 400 (bean validation on @RequestBody) ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.putIfAbsent(fe.getField(),
                        fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiError.validation("Request validation failed", req.getRequestURI(), fieldErrors));
    }

    // ---- 400 (bean validation on @RequestParam/@PathVariable via @Validated) ----
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            fieldErrors.putIfAbsent(String.valueOf(v.getPropertyPath()), v.getMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiError.validation("Request validation failed", req.getRequestURI(), fieldErrors));
    }

    // ---- 402 (quota / insufficient balance) ----
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiError> handleQuota(QuotaExceededException ex, HttpServletRequest req) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.PAYMENT_REQUIRED.value(),
                "Payment Required", ex.getMessage(), req.getRequestURI(), null, ex.blockType(), null);
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body);
    }

    // ---- 429 ----
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex, HttpServletRequest req) {
        return respond(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), req);
    }

    // ---- 413 ----
    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ApiError> handlePayloadTooLarge(PayloadTooLargeException ex, HttpServletRequest req) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), req);
    }

    // ---- 503 ----
    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ApiError> handleUpstream(UpstreamServiceException ex, HttpServletRequest req) {
        log.warn("Upstream dependency failure on {}: {}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    // ---- 503 (AI provider failure with a normalized error code, SPEC-24 §2) ----
    @ExceptionHandler(OpenRouterException.class)
    public ResponseEntity<ApiError> handleOpenRouter(OpenRouterException ex, HttpServletRequest req) {
        log.warn("OpenRouter failure on {}: {} ({})", req.getRequestURI(), ex.getMessage(), ex.error());
        ApiError body = new ApiError(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable", ex.error().name() + ": " + ex.getMessage(), req.getRequestURI(),
                null, ex.error().name(), null);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    // ---- Spring MVC exceptions that already carry a status (e.g. NoResourceFoundException → 404,
    //      method-not-allowed → 405, unsupported-media-type → 415). Honor their status. ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        // Spring MVC's status-carrying exceptions implement ErrorResponse (NoResourceFoundException
        // extends ServletException, not ErrorResponseException, so match the interface) — honor it.
        if (ex instanceof ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
            if (status.is5xxServerError()) {
                log.error("Server error on {} {}", req.getMethod(), req.getRequestURI(), ex);
            }
            ApiError body = ApiError.of(status.value(), reason, reason, req.getRequestURI());
            return ResponseEntity.status(status).body(body);
        }
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        ApiError body = new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error", "An unexpected error occurred", req.getRequestURI(),
                null, null, ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest req) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
                message == null ? status.getReasonPhrase() : message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
