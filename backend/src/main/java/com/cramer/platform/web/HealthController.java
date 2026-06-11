package com.cramer.platform.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liveness and readiness endpoints (SPEC-18 §8). Replaces the old ad-hoc
 * {@code HelloController} / {@code DatabaseTestController} / {@code DebugController}. Both routes
 * are public (SPEC-04 §1.1).
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Liveness: the process is up and serving requests. */
    @GetMapping
    public Map<String, Object> live() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        return body;
    }

    /** Readiness: dependencies (database) are reachable. Returns 503 when the DB check fails. */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            body.put("status", "UP");
            body.put("db", "UP");
            return ResponseEntity.ok(body);
        } catch (RuntimeException ex) {
            log.warn("Readiness DB check failed: {}", ex.getMessage());
            body.put("status", "DOWN");
            body.put("db", "DOWN");
            return ResponseEntity.status(503).body(body);
        }
    }
}
