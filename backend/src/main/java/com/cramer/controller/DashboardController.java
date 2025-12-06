package com.cramer.controller;

import com.cramer.dto.DashboardSummaryDTO;
import com.cramer.dto.TargetDTO;
import com.cramer.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary/{userId}")
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dashboardService.buildDashboardSummary(userId, page, size, search));
    }

    @PostMapping("/target")
    public ResponseEntity<TargetDTO> saveTarget(
            @RequestBody TargetDTO targetDTO,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID userId = UUID.fromString(authentication.getName());
        TargetDTO savedTarget = dashboardService.saveTarget(userId, targetDTO);
        return ResponseEntity.ok(savedTarget);
    }
}