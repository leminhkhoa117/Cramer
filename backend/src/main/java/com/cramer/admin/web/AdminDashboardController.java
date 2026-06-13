package com.cramer.admin.web;

import com.cramer.admin.service.AdminDashboardService;
import com.cramer.admin.web.dto.AdminDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin dashboard (SPEC-17 §3): top-level counts, recent activity, and system status.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboard;

    public AdminDashboardController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/stats")
    public AdminDtos.DashboardStatsView stats() {
        return dashboard.stats();
    }

    @GetMapping("/activities")
    public List<AdminDtos.AuditLogView> activities(@RequestParam(defaultValue = "20") int limit) {
        return dashboard.recentActivities(limit);
    }

    @GetMapping("/status")
    public AdminDtos.SystemStatusView status() {
        return dashboard.systemStatus();
    }
}
