package com.cramer.admin.web;

import com.cramer.admin.service.AdminActivityService;
import com.cramer.admin.web.dto.AdminDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin activity + audit reads (SPEC-17 §4): the admin audit trail and per-user activity timelines.
 */
@RestController
@RequestMapping("/api/admin/activities")
public class AdminActivityController {

    private final AdminActivityService activities;

    public AdminActivityController(AdminActivityService activities) {
        this.activities = activities;
    }

    @GetMapping("/audit")
    public List<AdminDtos.AuditLogView> audit(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return activities.auditLog(page, size);
    }

    @GetMapping("/audit/users/{userId}")
    public List<AdminDtos.AuditLogView> userAudit(@PathVariable UUID userId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return activities.userAudit(userId, page, size);
    }

    @GetMapping("/users/{userId}")
    public List<AdminDtos.UserActivityView> userActivities(@PathVariable UUID userId,
                                                           @RequestParam(required = false) String type,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return activities.userActivities(userId, type, page, size);
    }

    @GetMapping("/users/{userId}/recent")
    public List<AdminDtos.UserActivityView> userActivitiesRecent(@PathVariable UUID userId,
                                                                 @RequestParam(defaultValue = "10") int limit) {
        return activities.userActivitiesRecent(userId, limit);
    }
}
