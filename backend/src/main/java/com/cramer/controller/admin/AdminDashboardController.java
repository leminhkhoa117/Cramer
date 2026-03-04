package com.cramer.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AdminDashboardController - API endpoints cho Admin Dashboard
 * Cung cấp thống kê tổng quan, hoạt động gần đây và trạng thái hệ thống
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total users
            Long totalUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.profiles", Long.class
            );
            stats.put("totalUsers", totalUsers != null ? totalUsers : 0);
            
            // Active users in last 30 days
            Long activeUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM public.test_attempts WHERE started_at > NOW() - INTERVAL '30 days'",
                Long.class
            );
            stats.put("activeUsers", activeUsers != null ? activeUsers : 0);
            
            // New users this month
            Long newUsersThisMonth = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.profiles WHERE created_at >= DATE_TRUNC('month', NOW())",
                Long.class
            );
            stats.put("newUsersThisMonth", newUsersThisMonth != null ? newUsersThisMonth : 0);
            
            // Total test attempts
            Long totalTestAttempts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.test_attempts", Long.class
            );
            stats.put("totalTestAttempts", totalTestAttempts != null ? totalTestAttempts : 0);
            
            // Total questions
            Long totalQuestions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.questions", Long.class
            );
            stats.put("totalQuestions", totalQuestions != null ? totalQuestions : 0);
            
            // Published tests (count distinct exam_source + test_number combinations)
            Long publishedTests = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT CONCAT(exam_source, '_', test_number)) FROM public.sections",
                Long.class
            );
            stats.put("publishedTests", publishedTests != null ? publishedTests : 0);
            
            // Total vocabulary
            Long totalVocabulary = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.vocabulary", Long.class
            );
            stats.put("totalVocabulary", totalVocabulary != null ? totalVocabulary : 0);
            
            // Total revenue from payment orders (if table exists)
            try {
                Long totalRevenue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM public.payment_orders WHERE status = 'completed'",
                    Long.class
                );
                stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0);
            } catch (Exception e) {
                stats.put("totalRevenue", 0);
            }
            
            // Changes (mock for now - can be calculated by comparing with previous period)
            Map<String, Object> changes = new HashMap<>();
            changes.put("users", Map.of("value", 12.5, "type", "up"));
            changes.put("revenue", Map.of("value", 8.3, "type", "up"));
            changes.put("tests", Map.of("value", 4, "type", "up"));
            changes.put("growth", Map.of("value", 15.2, "type", "up"));
            stats.put("changes", changes);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching dashboard stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get recent activities for dashboard
     */
    @GetMapping("/activities")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivities(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            // Get recent user activities
            List<Map<String, Object>> activities = jdbcTemplate.queryForList(
                """
                SELECT 
                    ua.id,
                    ua.activity_type as type,
                    ua.title as action,
                    ua.description as detail,
                    p.username as user,
                    ua.created_at
                FROM public.user_activities ua
                LEFT JOIN public.profiles p ON ua.user_id = p.id
                ORDER BY ua.created_at DESC
                LIMIT ?
                """,
                limit
            );
            
            // Format time as relative (e.g., "5 phút trước")
            activities.forEach(activity -> {
                activity.put("time", formatRelativeTime(activity.get("created_at")));
            });
            
            return ResponseEntity.ok(activities);
            
        } catch (Exception e) {
            log.error("Error fetching recent activities", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get system status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getSystemStatus() {
        Map<String, String> status = new HashMap<>();
        
        // Check database connection
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "operational");
        } catch (Exception e) {
            status.put("database", "down");
        }
        
        // API server is operational if this endpoint responds
        status.put("apiServer", "operational");
        
        // Payment gateway - mock for now
        status.put("paymentGateway", "operational");
        
        // AI grading - mock for now
        status.put("aiGrading", "operational");
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Format timestamp as relative time string
     */
    private String formatRelativeTime(Object timestamp) {
        if (timestamp == null) return "Vừa xong";
        
        try {
            java.time.temporal.Temporal temporal;
            if (timestamp instanceof java.sql.Timestamp) {
                temporal = ((java.sql.Timestamp) timestamp).toInstant();
            } else if (timestamp instanceof java.time.OffsetDateTime) {
                temporal = (java.time.OffsetDateTime) timestamp;
            } else {
                return timestamp.toString();
            }
            
            long minutes = java.time.Duration.between(
                (java.time.Instant) temporal, 
                java.time.Instant.now()
            ).toMinutes();
            
            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (minutes < 1440) return (minutes / 60) + " giờ trước";
            return (minutes / 1440) + " ngày trước";
            
        } catch (Exception e) {
            return "N/A";
        }
    }
}
