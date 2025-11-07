package com.cramer.controller;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cramer.dto.DashboardSummaryDTO;
import com.cramer.service.DashboardService;


@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary/{userId}")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@PathVariable UUID userId){
        return ResponseEntity.ok(dashboardService.buildDashboardSummary(userId));
    }
}
