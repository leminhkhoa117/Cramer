package com.cramer.engagement.web;

import com.cramer.engagement.service.DashboardService;
import com.cramer.engagement.service.TargetService;
import com.cramer.engagement.web.dto.DashboardDtos;
import com.cramer.engagement.web.dto.TargetRequest;
import com.cramer.engagement.web.dto.TargetView;
import com.cramer.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Dashboard endpoints (SPEC-16 §4/§5): the aggregate summary + per-attempt course history
 * (read-only projections) and the IELTS goal (target) upsert/read (engagement-owned data).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;
    private final TargetService targets;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService dashboard, TargetService targets, CurrentUser currentUser) {
        this.dashboard = dashboard;
        this.targets = targets;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public DashboardDtos.SummaryView summary(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String search) {
        return dashboard.summary(currentUser.requireUserId(), page, size, search);
    }

    @GetMapping("/course-history")
    public List<DashboardDtos.CourseHistoryItem> courseHistory(@RequestParam(required = false) String examSource,
                                                               @RequestParam(required = false) String testNumber,
                                                               @RequestParam(required = false) String skill) {
        return dashboard.courseHistory(currentUser.requireUserId(), examSource, testNumber, skill);
    }

    @GetMapping("/target")
    public Map<String, TargetView> getTarget() {
        return Map.of("target", targets.current(currentUser.requireUserId()).orElse(null));
    }

    @PostMapping("/target")
    public TargetView upsertTarget(@Valid @RequestBody TargetRequest request) {
        return targets.upsert(currentUser.requireUserId(), request);
    }
}
