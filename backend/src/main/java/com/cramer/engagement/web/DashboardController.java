package com.cramer.engagement.web;

import com.cramer.engagement.service.TargetService;
import com.cramer.engagement.web.dto.TargetRequest;
import com.cramer.engagement.web.dto.TargetView;
import com.cramer.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard endpoints (SPEC-16 §4/§5). The heavy cross-module summary/course-history read-models
 * are assembled at integration time; this controller owns the IELTS goal (target) upsert/read,
 * which is engagement-owned data.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TargetService targets;
    private final CurrentUser currentUser;

    public DashboardController(TargetService targets, CurrentUser currentUser) {
        this.targets = targets;
        this.currentUser = currentUser;
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
