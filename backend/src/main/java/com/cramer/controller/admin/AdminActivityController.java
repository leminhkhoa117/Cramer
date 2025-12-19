package com.cramer.controller.admin;

import com.cramer.dto.AdminAuditLogDTO;
import com.cramer.dto.UserActivityDTO;
import com.cramer.service.AdminAuditService;
import com.cramer.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
public class AdminActivityController {

    private final UserActivityService userActivityService;
    private final AdminAuditService adminAuditService;

    /**
     * Lấy hoạt động của user
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<UserActivityDTO>> getUserActivities(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        
        UUID uuid = UUID.fromString(userId);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<UserActivityDTO> activities = type != null
                ? userActivityService.getUserActivitiesByType(uuid, type, pageable)
                : userActivityService.getUserActivities(uuid, pageable);
        
        return ResponseEntity.ok(activities);
    }

    /**
     * Lấy hoạt động gần đây của user
     */
    @GetMapping("/users/{userId}/recent")
    public ResponseEntity<List<UserActivityDTO>> getRecentActivities(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        UUID uuid = UUID.fromString(userId);
        List<UserActivityDTO> activities = userActivityService.getRecentActivities(uuid, limit);
        
        return ResponseEntity.ok(activities);
    }

    /**
     * Lấy audit logs của user
     */
    @GetMapping("/audit/users/{userId}")
    public ResponseEntity<Page<AdminAuditLogDTO>> getAuditLogsForUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminAuditLogDTO> logs = adminAuditService.getAuditLogsForUser(userId, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Lấy tất cả audit logs
     */
    @GetMapping("/audit")
    public ResponseEntity<Page<AdminAuditLogDTO>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminAuditLogDTO> logs = adminAuditService.getAllAuditLogs(pageable);
        
        return ResponseEntity.ok(logs);
    }
}