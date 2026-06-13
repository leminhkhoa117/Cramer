package com.cramer.admin.service;

import com.cramer.admin.web.dto.AdminDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin finance projections (SPEC-17 §5). <strong>Consistent data source:</strong> all queries
 * use {@code payment_orders.status = 'PAID'} and {@code amount_vnd} (no stale
 * {@code status='completed'}/{@code amount} path). Read-only.
 */
@Service
@Transactional(readOnly = true)
public class AdminFinanceService {

    private final JdbcTemplate jdbc;

    public AdminFinanceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AdminDtos.FinanceOverviewView overview(String period) {
        String since = sinceClause(period);
        long total = scalar("SELECT COALESCE(SUM(amount_vnd),0) FROM payment_orders WHERE status='PAID'" + since);
        long orders = scalar("SELECT COUNT(*) FROM payment_orders WHERE status='PAID'" + since);
        long subs = scalar("SELECT COALESCE(SUM(amount_vnd),0) FROM payment_orders WHERE status='PAID' AND type='SUBSCRIPTION'" + since);
        long lua = scalar("SELECT COALESCE(SUM(amount_vnd),0) FROM payment_orders WHERE status='PAID' AND type='LUA_PACK'" + since);
        return new AdminDtos.FinanceOverviewView(total, orders, subs, lua);
    }

    public AdminDtos.RevenueBreakdownView breakdown(String period) {
        String since = sinceClause(period);
        List<AdminDtos.RevenueSlice> slices = jdbc.query(
                "SELECT type, COALESCE(SUM(amount_vnd),0) revenue, COUNT(*) orders "
                        + "FROM payment_orders WHERE status='PAID'" + since + " GROUP BY type ORDER BY revenue DESC",
                (rs, i) -> new AdminDtos.RevenueSlice(rs.getString("type"), rs.getLong("revenue"), rs.getLong("orders")));
        return new AdminDtos.RevenueBreakdownView(new ArrayList<>(slices));
    }

    public List<AdminDtos.TopSpenderView> topSpenders(int limit) {
        int capped = Math.min(Math.max(limit, 1), 100);
        return jdbc.query("""
                SELECT o.user_id, p.username, COALESCE(SUM(o.amount_vnd),0) total, COUNT(*) orders
                FROM payment_orders o
                LEFT JOIN profiles p ON p.id = o.user_id
                WHERE o.status = 'PAID'
                GROUP BY o.user_id, p.username
                ORDER BY total DESC
                LIMIT ?
                """, (rs, i) -> new AdminDtos.TopSpenderView(
                rs.getString("user_id"), rs.getString("username"), rs.getLong("total"), rs.getLong("orders")), capped);
    }

    public List<AdminDtos.AuditLogView> transactions(String status, String type, int page, int size) {
        int capped = Math.min(Math.max(size, 1), 100);
        StringBuilder sql = new StringBuilder("""
                SELECT order_code AS id, user_id, type, status, amount_vnd, created_at
                FROM payment_orders WHERE 1=1""");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status.trim().toUpperCase());
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            args.add(type.trim().toUpperCase());
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        args.add(capped);
        args.add(Math.max(page, 0) * capped);
        return jdbc.query(sql.toString(), (rs, i) -> new AdminDtos.AuditLogView(
                rs.getLong("id"), rs.getString("user_id"), rs.getString("type"), "PAYMENT",
                rs.getString("status"), "amount_vnd=" + rs.getLong("amount_vnd"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)), args.toArray());
    }

    /** Whitelisted period → SQL fragment (never raw user input). */
    private String sinceClause(String period) {
        return switch (period == null ? "30d" : period.toLowerCase()) {
            case "7d" -> " AND created_at >= now() - interval '7 days'";
            case "90d" -> " AND created_at >= now() - interval '90 days'";
            case "all" -> "";
            default -> " AND created_at >= now() - interval '30 days'";
        };
    }

    private long scalar(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
