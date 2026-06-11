package com.cramer.admin.service;

import com.cramer.admin.web.dto.AdminDtos;
import com.cramer.billing.service.PaymentService;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin dashboard projections (SPEC-17 §3). Real counts from the live tables (no faked metrics);
 * system status reflects DB reachability and integration configuration.
 */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final JdbcTemplate jdbc;
    private final PaymentService payments;
    private final OpenRouterClient openRouter;

    public AdminDashboardService(JdbcTemplate jdbc, PaymentService payments, OpenRouterClient openRouter) {
        this.jdbc = jdbc;
        this.payments = payments;
        this.openRouter = openRouter;
    }

    public AdminDtos.DashboardStatsView stats() {
        return new AdminDtos.DashboardStatsView(
                count("SELECT COUNT(*) FROM profiles"),
                count("SELECT COUNT(*) FROM user_subscriptions WHERE status = 'ACTIVE'"),
                count("""
                        SELECT COUNT(DISTINCT s.user_id) FROM user_subscriptions s
                        JOIN subscription_tiers t ON t.id = s.tier_id
                        WHERE s.status = 'ACTIVE' AND t.price_vnd > 0"""),
                count("SELECT COUNT(*) FROM test_attempts"),
                count("SELECT COUNT(*) FROM writing_submissions"),
                count("SELECT COUNT(*) FROM payment_orders WHERE status = 'PAID'"),
                count("SELECT COALESCE(SUM(amount_vnd),0) FROM payment_orders WHERE status = 'PAID'"));
    }

    public List<AdminDtos.AuditLogView> recentActivities(int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return jdbc.query("""
                SELECT id, admin_user_id, action, target_type, target_id, description, created_at
                FROM admin_audit_log ORDER BY created_at DESC LIMIT ?
                """, (rs, i) -> new AdminDtos.AuditLogView(
                rs.getLong("id"), rs.getString("admin_user_id"), rs.getString("action"),
                rs.getString("target_type"), rs.getString("target_id"), rs.getString("description"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)), capped);
    }

    public AdminDtos.SystemStatusView systemStatus() {
        String db;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            db = "UP";
        } catch (RuntimeException e) {
            db = "DOWN";
        }
        String payment = payments.payosConfigured() ? "CONFIGURED" : "MOCK";
        String ai = openRouter.isConfigured() ? "CONFIGURED" : "NOT_CONFIGURED";
        return new AdminDtos.SystemStatusView(db, payment, ai);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
