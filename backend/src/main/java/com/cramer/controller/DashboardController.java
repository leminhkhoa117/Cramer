package com.cramer.controller;

import com.cramer.dto.AttemptHistoryDTO;
import com.cramer.dto.DashboardSummaryDTO;
import com.cramer.dto.TargetDTO;
import com.cramer.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "3") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(50) int size,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        // Extract userId from authenticated user - no IDOR possible
        UUID userId = getCurrentUserId(authentication);
        // Cap size to prevent abuse
        int cappedSize = Math.min(size, 50);
        return ResponseEntity.ok(dashboardService.buildDashboardSummary(userId, page, cappedSize, search));
    }

    @GetMapping("/course-history")
    public ResponseEntity<List<AttemptHistoryDTO>> getCourseHistory(
            @RequestParam String examSource,
            @RequestParam Integer testNumber,
            @RequestParam String skill,
            Authentication authentication
    ) {
        UUID userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(dashboardService.getCourseHistory(userId, examSource, testNumber, skill));
    }

    @PostMapping("/target")
    public ResponseEntity<TargetDTO> saveTarget(
            @Valid @RequestBody TargetDTO targetDTO,
            Authentication authentication
    ) {
        // Redundant check removed - Spring Security handles authentication
        UUID userId = getCurrentUserId(authentication);
        TargetDTO savedTarget = dashboardService.saveTarget(userId, targetDTO);
        return ResponseEntity.ok(savedTarget);
    }
}