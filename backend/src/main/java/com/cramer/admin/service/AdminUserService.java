package com.cramer.admin.service;

import com.cramer.admin.web.dto.AdminDtos;
import com.cramer.billing.domain.CreditCategory;
import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.service.CreditService;
import com.cramer.billing.service.SubscriptionService;
import com.cramer.engagement.service.ActivityPort;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.ResourceNotFoundException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Admin user management (SPEC-17 §2). Reads are read-only JdbcTemplate projections across
 * {@code profiles}/{@code user_subscriptions}/{@code subscription_tiers}/{@code user_credits};
 * writes go through {@code billing} services (credits via {@link CreditService} with
 * {@code ADMIN_ADJUSTMENT}, subscription via {@link SubscriptionService}) plus the audit and
 * activity trails. Admin identity is the authenticated principal (no {@code X-User-Id}).
 */
@Service
public class AdminUserService {

    private static final List<String> SORTABLE = List.of("created_at", "username", "account_status");

    private final JdbcTemplate jdbc;
    private final CreditService creditService;
    private final SubscriptionService subscriptionService;
    private final AuditPort audit;
    private final ActivityPort activity;

    public AdminUserService(JdbcTemplate jdbc, CreditService creditService,
                            SubscriptionService subscriptionService, AuditPort audit, ActivityPort activity) {
        this.jdbc = jdbc;
        this.creditService = creditService;
        this.subscriptionService = subscriptionService;
        this.audit = audit;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.AdminUserView> listUsers(String search, String status, String sortBy,
                                                   String sortOrder, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.username, p.full_name, p.account_status, p.is_admin, p.created_at,
                       t.code AS tier_code, COALESCE(c.balance, 0) AS lua_balance
                FROM profiles p
                LEFT JOIN user_subscriptions s ON s.user_id = p.id AND s.status = 'ACTIVE'
                LEFT JOIN subscription_tiers t ON t.id = s.tier_id
                LEFT JOIN user_credits c ON c.user_id = p.id
                WHERE 1=1
                """);
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (p.username ILIKE ? OR p.full_name ILIKE ?)");
            String like = "%" + search.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND p.account_status = ?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        String orderCol = SORTABLE.contains(sortBy) ? sortBy : "created_at";
        String dir = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        sql.append(" ORDER BY ").append(orderCol).append(' ').append(dir);
        sql.append(" LIMIT ? OFFSET ?");
        args.add(Math.min(Math.max(size, 1), 100));
        args.add(Math.max(page, 0) * Math.min(Math.max(size, 1), 100));

        return jdbc.query(sql.toString(), (rs, i) -> new AdminDtos.AdminUserView(
                rs.getString("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("account_status"), rs.getBoolean("is_admin"), rs.getString("tier_code"),
                rs.getInt("lua_balance"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)), args.toArray());
    }

    @Transactional(readOnly = true)
    public AdminDtos.AdminUserDetailView userDetail(UUID userId) {
        return jdbc.query("""
                SELECT p.id, p.username, p.full_name, p.account_status, p.status_reason, p.is_admin, p.created_at,
                       t.code AS tier_code, s.status AS sub_status, s.expires_at,
                       COALESCE(c.balance,0) balance, COALESCE(c.lifetime_earned,0) earned, COALESCE(c.lifetime_spent,0) spent
                FROM profiles p
                LEFT JOIN user_subscriptions s ON s.user_id = p.id AND s.status = 'ACTIVE'
                LEFT JOIN subscription_tiers t ON t.id = s.tier_id
                LEFT JOIN user_credits c ON c.user_id = p.id
                WHERE p.id = ?
                """, rs -> {
            if (!rs.next()) {
                throw ResourceNotFoundException.of("User", userId);
            }
            return new AdminDtos.AdminUserDetailView(
                    rs.getString("id"), rs.getString("username"), rs.getString("full_name"),
                    rs.getString("account_status"), rs.getString("status_reason"), rs.getBoolean("is_admin"),
                    rs.getString("tier_code"), rs.getString("sub_status"),
                    rs.getObject("expires_at", java.time.OffsetDateTime.class),
                    rs.getInt("balance"), rs.getInt("earned"), rs.getInt("spent"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class));
        }, userId);
    }

    @Transactional(readOnly = true)
    public AdminDtos.UserStatsView userStats() {
        long total = count("SELECT COUNT(*) FROM profiles");
        long active = count("SELECT COUNT(*) FROM profiles WHERE account_status = 'ACTIVE'");
        long premium = count("""
                SELECT COUNT(DISTINCT s.user_id) FROM user_subscriptions s
                JOIN subscription_tiers t ON t.id = s.tier_id
                WHERE s.status = 'ACTIVE' AND t.price_vnd > 0""");
        long newThisMonth = count("SELECT COUNT(*) FROM profiles WHERE created_at >= date_trunc('month', now())");
        return new AdminDtos.UserStatsView(total, active, premium, newThisMonth);
    }

    @Transactional
    public void setStatus(UUID adminId, UUID userId, String status, String reason) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        int updated = jdbc.update("UPDATE profiles SET account_status = ?, status_reason = ? WHERE id = ?",
                normalized, reason, userId);
        if (updated == 0) {
            throw ResourceNotFoundException.of("User", userId);
        }
        ObjectNode newValue = Json.mapper().createObjectNode().put("status", normalized).put("reason", reason);
        audit.record(adminId, "STATUS_CHANGE", "USER", userId.toString(),
                "Account status set to " + normalized, null, newValue);
        activity.log(userId, "ACCOUNT_STATUS_CHANGED", "Account status updated",
                "Status set to " + normalized, null);
    }

    @Transactional
    public int adjustCredits(UUID adminId, UUID userId, int amount, String reason) {
        if (amount == 0) {
            throw new IllegalArgumentException("Adjustment amount must be non-zero");
        }
        String reference = "admin_adj_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(10000);
        int balanceAfter;
        if (amount > 0) {
            balanceAfter = creditService.earn(userId, amount, CreditCategory.ADMIN_ADJUSTMENT, reference,
                    reason == null ? "Admin credit adjustment" : reason).balanceAfter();
        } else {
            balanceAfter = creditService.spend(userId, -amount, CreditCategory.ADMIN_ADJUSTMENT, reference,
                    reason == null ? "Admin credit adjustment" : reason).balanceAfter();
        }
        ObjectNode newValue = Json.mapper().createObjectNode().put("amount", amount).put("balanceAfter", balanceAfter);
        audit.record(adminId, amount > 0 ? "CREDITS_ADD" : "CREDITS_SUBTRACT", "USER", userId.toString(),
                reason, null, newValue);
        activity.log(userId, "CREDITS_ADJUSTED", "Lúa adjusted by admin",
                (amount > 0 ? "+" : "") + amount + " Lúa", newValue);
        return balanceAfter;
    }

    @Transactional
    public void setSubscription(UUID adminId, UUID userId, String tierCode, int months) {
        SubscriptionTier tier = subscriptionService.getTierByCode(tierCode);
        subscriptionService.adminSetTier(userId, tier, months);
        ObjectNode newValue = Json.mapper().createObjectNode().put("tier", tier.getCode()).put("months", months);
        audit.record(adminId, "SUBSCRIPTION_CHANGE", "USER", userId.toString(),
                "Subscription set to " + tier.getCode(), null, newValue);
        activity.log(userId, "SUBSCRIPTION_CHANGED", "Subscription updated by admin",
                "Tier set to " + tier.getCode(), newValue);
    }

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }
}
