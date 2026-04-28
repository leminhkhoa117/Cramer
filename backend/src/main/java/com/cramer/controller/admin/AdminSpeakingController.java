package com.cramer.controller.admin;

import com.cramer.service.AdminSpeakingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/speaking")
public class AdminSpeakingController {

    private static final Logger logger = LoggerFactory.getLogger(AdminSpeakingController.class);

    private final AdminSpeakingService adminSpeakingService;

    public AdminSpeakingController(AdminSpeakingService adminSpeakingService) {
        this.adminSpeakingService = adminSpeakingService;
    }

    @PostMapping("/sessions/{id}/regrade")
    public ResponseEntity<Map<String, Object>> regrade(
            @PathVariable Long id,
            @RequestParam(defaultValue = "multimodal") String mode,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestHeader("X-User-Id") String adminUserId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        String authenticatedUserId = auth.getName();
        if (!authenticatedUserId.equals(adminUserId)) {
            logger.warn("X-User-Id mismatch: header={} vs JWT principal={}", adminUserId, authenticatedUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "X-User-Id does not match authenticated user"));
        }

        String reason = body.getOrDefault("reason", "");
        if (reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
        }

        logger.info("Admin {} requesting speaking regrade for session {} (mode={}, force={})", adminUserId, id, mode, force);

        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        Map<String, Object> result = adminSpeakingService.regrade(id, mode, force, UUID.fromString(adminUserId), reason, ipAddress, userAgent);
        return ResponseEntity.ok(result);
    }
}
