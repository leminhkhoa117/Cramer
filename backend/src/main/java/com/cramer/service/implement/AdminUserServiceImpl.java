package com.cramer.service.implement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.cramer.dto.AdminUserDTO;
import com.cramer.exception.ResourceNotFoundException;
import com.cramer.dto.AdminUserListResponse;
import com.cramer.service.AdminAuditService;
import com.cramer.service.AdminUserService;
import com.cramer.service.UserActivityService;

/**
 * Admin User Service Implementation - Xử lý logic quản lý users cho Admin CMS
 * 
 * Sử dụng JdbcTemplate để query trực tiếp từ database Supabase.
 * Kết hợp dữ liệu từ nhiều bảng: profiles, user_credits, user_subscriptions,
 * subscription_tiers, auth.users.
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private AdminAuditService adminAuditService;

    @Override
    public AdminUserListResponse getUsers(int page, int size, String search, String status,
            String subscription, String sortBy, String sortOrder) {
        try {
            // Build dynamic SQL query
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT ");
            sqlBuilder.append("  p.id, ");
            sqlBuilder.append("  p.username, ");
            sqlBuilder.append("  p.full_name, ");
            sqlBuilder.append("  p.phone_number, ");
            sqlBuilder.append("  p.address, ");
            sqlBuilder.append("  p.avatar_url, ");
            sqlBuilder.append("  p.created_at, ");
            sqlBuilder.append("  COALESCE(p.account_status, 'ACTIVE') as account_status, ");
            sqlBuilder.append("  p.last_login_at, ");
            sqlBuilder.append("  p.status_reason, ");
            sqlBuilder.append("  COALESCE(uc.balance, 0) as credits, ");
            sqlBuilder.append("  st.code as subscription_tier, ");
            sqlBuilder.append("  us.status as subscription_status, ");
            sqlBuilder.append("  us.started_at as current_period_start, ");
            sqlBuilder.append("  us.expires_at as current_period_end, ");
            sqlBuilder.append("  COALESCE(us.auto_renew, false) as auto_renew, ");
            sqlBuilder.append("  au.email ");
            sqlBuilder.append("FROM public.profiles p ");
            sqlBuilder.append("LEFT JOIN public.user_credits uc ON p.id = uc.user_id ");
            sqlBuilder.append("LEFT JOIN public.user_subscriptions us ON p.id = us.user_id ");
            sqlBuilder.append("LEFT JOIN public.subscription_tiers st ON us.tier_id = st.id ");
            sqlBuilder.append("LEFT JOIN auth.users au ON p.id = au.id ");
            sqlBuilder.append("WHERE 1=1 ");

            List<Object> params = new ArrayList<>();

            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                sqlBuilder.append("AND (p.username ILIKE ? OR p.full_name ILIKE ? OR au.email ILIKE ?) ");
                String searchPattern = "%" + search.trim() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
                params.add(searchPattern);
            }

            // Status filter
            if (status != null && !status.equals("ALL")) {
                sqlBuilder.append("AND COALESCE(p.account_status, 'ACTIVE') = ? ");
                params.add(status);
            }

            // Subscription filter
            if (subscription != null && !subscription.equals("ALL")) {
                if (subscription.equals("FREE") || subscription.equals("CRAMERIE")) {
                    sqlBuilder.append("AND (st.code IS NULL OR st.code = 'cramerie') ");
                } else if (subscription.equals("CRAMERICH")) {
                    sqlBuilder.append("AND st.code = 'cramerich' ");
                }
            }

            // Count total
            StringBuilder countBuilder = new StringBuilder();
            countBuilder.append("SELECT COUNT(*) FROM public.profiles p ");
            countBuilder.append("LEFT JOIN public.user_subscriptions us ON p.id = us.user_id ");
            countBuilder.append("LEFT JOIN public.subscription_tiers st ON us.tier_id = st.id ");
            countBuilder.append("LEFT JOIN auth.users au ON p.id = au.id ");
            countBuilder.append("WHERE 1=1 ");

            List<Object> countParams = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                countBuilder.append("AND (p.username ILIKE ? OR p.full_name ILIKE ? OR au.email ILIKE ?) ");
                String searchPattern = "%" + search.trim() + "%";
                countParams.add(searchPattern);
                countParams.add(searchPattern);
                countParams.add(searchPattern);
            }

            if (status != null && !status.equals("ALL")) {
                countBuilder.append("AND COALESCE(p.account_status, 'ACTIVE') = ? ");
                countParams.add(status);
            }

            if (subscription != null && !subscription.equals("ALL")) {
                if (subscription.equals("FREE") || subscription.equals("CRAMERIE")) {
                    countBuilder.append("AND (st.code IS NULL OR st.code = 'cramerie') ");
                } else if (subscription.equals("CRAMERICH")) {
                    countBuilder.append("AND st.code = 'cramerich' ");
                }
            }

            long totalItems = jdbcTemplate.queryForObject(Objects.requireNonNull(countBuilder.toString()), Long.class,
                    countParams.toArray());

            // Sorting
            String sortColumn = getSortColumn(sortBy);
            String order = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
            sqlBuilder.append("ORDER BY ").append(sortColumn).append(" ").append(order).append(" NULLS LAST ");

            // Pagination
            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);

            // Execute query
            List<AdminUserDTO> users = jdbcTemplate.query(
                    Objects.requireNonNull(sqlBuilder.toString()),
                    this::mapRowToAdminUserDTO,
                    params.toArray());

            return new AdminUserListResponse(users, page, size, totalItems);

        } catch (Exception e) {
            logger.error("Error fetching users for admin", e);
            return new AdminUserListResponse(new ArrayList<>(), page, size, 0);
        }
    }

    @Override
    public AdminUserDTO getUserById(String id) {
        try {
            String sql = """
                        SELECT
                            p.id,
                            p.username,
                            p.full_name,
                            p.phone_number,
                            p.address,
                            p.avatar_url,
                            p.created_at,
                            COALESCE(p.account_status, 'ACTIVE') as account_status,
                            p.last_login_at,
                            p.status_reason,
                            COALESCE(uc.balance, 0) as credits,
                            st.code as subscription_tier,
                            us.status as subscription_status,
                            us.started_at as current_period_start,
                            us.expires_at as current_period_end,
                            COALESCE(us.auto_renew, false) as auto_renew,
                            au.email,
                            (SELECT COUNT(*) FROM public.test_attempts ta WHERE ta.user_id = p.id) as total_tests,
                            (SELECT COUNT(*) FROM public.vocabulary v WHERE v.user_id = p.id) as total_vocabulary
                        FROM public.profiles p
                        LEFT JOIN public.user_credits uc ON p.id = uc.user_id
                        LEFT JOIN public.user_subscriptions us ON p.id = us.user_id
                        LEFT JOIN public.subscription_tiers st ON us.tier_id = st.id
                        LEFT JOIN auth.users au ON p.id = au.id
                        WHERE p.id = ?::uuid
                    """;

            List<AdminUserDTO> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                AdminUserDTO dto = mapRowToAdminUserDTO(rs, rowNum);
                dto.setTotalTests(rs.getInt("total_tests"));
                dto.setTotalVocabulary(rs.getInt("total_vocabulary"));
                return dto;
            }, id);

            return results.isEmpty() ? null : results.get(0);

        } catch (Exception e) {
            logger.error("Error fetching user by id: " + id, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Total users
            Long totalUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.profiles", Long.class);
            stats.put("totalUsers", totalUsers);

            // Active users (users who have test attempts in last 30 days)
            Long activeUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM public.test_attempts WHERE started_at > NOW() - INTERVAL '30 days'",
                    Long.class);
            stats.put("activeUsers", activeUsers);

            // Premium users (Cramerich with active status)
            Long premiumUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.user_subscriptions us " +
                            "JOIN public.subscription_tiers st ON us.tier_id = st.id " +
                            "WHERE st.code = 'cramerich' AND us.status = 'active'",
                    Long.class);
            stats.put("premiumUsers", premiumUsers);

            // New users this month
            Long newUsersThisMonth = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.profiles WHERE created_at >= DATE_TRUNC('month', NOW())",
                    Long.class);
            stats.put("newUsersThisMonth", newUsersThisMonth);

        } catch (Exception e) {
            logger.error("Error fetching user stats", e);
            stats.put("totalUsers", 0);
            stats.put("activeUsers", 0);
            stats.put("premiumUsers", 0);
            stats.put("newUsersThisMonth", 0);
        }

        return stats;
    }

    @Override
    public AdminUserDTO updateUserStatus(String userId, String newStatus, String reason, String adminId) {
        try {
            // Get current status before update
            String oldStatus = "ACTIVE";
            String adminEmail = null;
            try {
                oldStatus = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(account_status, 'ACTIVE') FROM public.profiles WHERE id = ?::uuid",
                        String.class, userId);
                adminEmail = jdbcTemplate.queryForObject(
                        "SELECT email FROM auth.users WHERE id = ?::uuid",
                        String.class, adminId);
            } catch (Exception e) {
                logger.warn("Could not get old status or admin email: " + e.getMessage());
            }

            // Update status
            jdbcTemplate.update(
                    "UPDATE public.profiles SET account_status = ?, status_reason = ? WHERE id = ?::uuid",
                    newStatus, reason, userId);

            // Log to Admin Audit
            try {
                adminAuditService.logStatusChange(
                        UUID.fromString(adminId),
                        adminEmail,
                        userId,
                        oldStatus,
                        newStatus,
                        reason,
                        null // IP address - could be passed from controller
                );
            } catch (Exception e) {
                logger.warn("Could not log audit: " + e.getMessage());
            }

            logger.info("Admin {} updated user {} status to {} with reason: {}", adminId, userId, newStatus, reason);

            return getUserById(userId);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating user status: " + userId, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    public AdminUserDTO updateUserCredits(String userId, int amount, String action, String reason, String adminId) {
        try {
            // Get current balance and admin email
            Integer currentBalance = 0;
            String adminEmail = null;
            try {
                currentBalance = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(balance, 0) FROM public.user_credits WHERE user_id = ?::uuid",
                        Integer.class,
                        userId);
                adminEmail = jdbcTemplate.queryForObject(
                        "SELECT email FROM auth.users WHERE id = ?::uuid",
                        String.class, adminId);
            } catch (Exception e) {
                // If query fails, currentBalance will be its initial value (0)
                // Nothing to do here if we accept 0 as default
                logger.warn("Could not get balance or admin email: " + e.getMessage());
            }

            int oldBalance = currentBalance != null ? currentBalance : 0;
            int newBalance;
            int actualAmount;

            if ("ADD".equalsIgnoreCase(action)) {
                newBalance = oldBalance + amount;
                actualAmount = amount;
            } else {
                newBalance = Math.max(0, oldBalance - amount);
                actualAmount = -amount;
            }

            // Update or insert credits
            int updated = jdbcTemplate.update(
                    "UPDATE public.user_credits SET balance = ?, updated_at = NOW() WHERE user_id = ?::uuid",
                    newBalance, userId);

            if (updated == 0) {
                // Insert new record
                jdbcTemplate.update(
                        "INSERT INTO public.user_credits (user_id, balance, lifetime_earned, lifetime_spent, created_at, updated_at) "
                                +
                                "VALUES (?::uuid, ?, 0, 0, NOW(), NOW())",
                        userId, newBalance);
            }

            // Log the credit change to credit_transactions
            try {
                jdbcTemplate.update(
                        "INSERT INTO public.credit_transactions (user_id, amount, type, category, description, created_at) "
                                +
                                "VALUES (?::uuid, ?, 'BONUS', 'ADMIN_ADJUSTMENT', ?, NOW())",
                        userId,
                        actualAmount,
                        "Admin: " + reason);
            } catch (Exception e) {
                logger.warn("Could not log credit transaction: " + e.getMessage());
            }

            // Log to User Activities (user can see this)
            try {
                userActivityService.logCreditsChanged(
                        UUID.fromString(userId),
                        actualAmount,
                        reason);
            } catch (Exception e) {
                logger.warn("Could not log user activity: " + e.getMessage());
            }

            // Log to Admin Audit Log (admin can see this)
            try {
                adminAuditService.logCreditsChange(
                        UUID.fromString(adminId),
                        adminEmail,
                        userId,
                        oldBalance,
                        newBalance,
                        actualAmount,
                        reason,
                        null // IP address
                );
            } catch (Exception e) {
                logger.warn("Could not log admin audit: " + e.getMessage());
            }

            logger.info("Admin {} {} {} Lúa for user {} with reason: {}",
                    adminId, action, amount, userId, reason);

            return getUserById(userId);

        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating credits for user: " + userId, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    public AdminUserDTO updateUserSubscription(String userId, String newTierCode, int durationMonths, String reason, String adminId) {
        try {
            logger.info("Admin {} changing subscription for user {} to tier: {} for {} months", adminId, userId, newTierCode, durationMonths);

            // Get admin email for audit logging
            String adminEmail = null;
            try {
                adminEmail = jdbcTemplate.queryForObject(
                        "SELECT email FROM auth.users WHERE id = ?::uuid",
                        String.class, adminId);
            } catch (Exception e) {
                logger.warn("Could not get admin email: " + e.getMessage());
            }

            // Get old tier info
            String oldTierCode = "cramerie";
            try {
                oldTierCode = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(st.code, 'cramerie') FROM public.user_subscriptions us " +
                        "JOIN public.subscription_tiers st ON us.tier_id = st.id " +
                        "WHERE us.user_id = ?::uuid",
                        String.class, userId);
            } catch (Exception e) {
                logger.info("No existing subscription for user, assuming cramerie");
            }

            // Get new tier ID
            Long newTierId;
            try {
                newTierId = jdbcTemplate.queryForObject(
                        "SELECT id FROM public.subscription_tiers WHERE code = ? AND is_active = true",
                        Long.class, newTierCode);
            } catch (Exception e) {
                logger.error("Tier not found: " + newTierCode);
                throw new IllegalArgumentException("Invalid tier code: " + newTierCode);
            }

            // Calculate expiry date based on duration for paid tiers, far future for free tier
            java.sql.Timestamp expiresAt = null;
            if ("cramerich".equalsIgnoreCase(newTierCode)) {
                // Validate duration: 1, 3, or 6 months
                int validDuration = (durationMonths == 1 || durationMonths == 3 || durationMonths == 6) ? durationMonths : 1;
                java.time.OffsetDateTime expiry = java.time.OffsetDateTime.now().plusMonths(validDuration);
                expiresAt = java.sql.Timestamp.from(expiry.toInstant());
                logger.info("Setting subscription expiry to {} ({} months from now)", expiry, validDuration);
            } else {
                // Free tier: set expiry to far future (effectively never expires)
                java.time.OffsetDateTime farFuture = java.time.OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, java.time.ZoneOffset.UTC);
                expiresAt = java.sql.Timestamp.from(farFuture.toInstant());
                logger.info("Setting free tier subscription with far future expiry: {}", farFuture);
            }

            // Check if user has existing subscription
            Integer existingSubCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.user_subscriptions WHERE user_id = ?::uuid",
                    Integer.class, userId);

            if (existingSubCount != null && existingSubCount > 0) {
                // Update existing subscription (ai_gradings_used column removed)
                jdbcTemplate.update(
                        "UPDATE public.user_subscriptions " +
                        "SET tier_id = ?, status = 'ACTIVE', started_at = NOW(), expires_at = ?, " +
                        "attempts_used = 0, attempt_ais_used = 0, chatbot_used = 0, " +
                        "auto_renew = false " +
                        "WHERE user_id = ?::uuid",
                        newTierId, expiresAt, userId);
            } else {
                // Insert new subscription (ai_gradings_used column removed)
                jdbcTemplate.update(
                        "INSERT INTO public.user_subscriptions " +
                        "(user_id, tier_id, status, started_at, expires_at, attempts_used, attempt_ais_used, " +
                        "chatbot_used, auto_renew) " +
                        "VALUES (?::uuid, ?, 'ACTIVE', NOW(), ?, 0, 0, 0, false)",
                        userId, newTierId, expiresAt);
            }

            // Log to User Activities
            try {
                userActivityService.logSubscriptionChanged(
                        UUID.fromString(userId),
                        oldTierCode,
                        newTierCode);
            } catch (Exception e) {
                logger.warn("Could not log user activity: " + e.getMessage());
            }

            // Log to Admin Audit
            try {
                adminAuditService.logSubscriptionChange(
                        UUID.fromString(adminId),
                        adminEmail,
                        userId,
                        oldTierCode,
                        newTierCode,
                        null);
            } catch (Exception e) {
                logger.warn("Could not log admin audit: " + e.getMessage());
            }

            logger.info("Successfully changed subscription for user {} from {} to {} (expires: {})",
                    userId, oldTierCode, newTierCode, expiresAt);

            return getUserById(userId);

        } catch (IllegalArgumentException e) {
            // T10 (BUG_AUDIT): re-throw so GlobalExceptionHandler returns 400, not 500
            throw e;
        } catch (Exception e) {
            logger.error("Error updating subscription for user: " + userId, e);
            throw new RuntimeException("Failed to update subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Map database row to AdminUserDTO
     */
    private AdminUserDTO mapRowToAdminUserDTO(ResultSet rs, int rowNum) throws SQLException {
        AdminUserDTO dto = new AdminUserDTO();

        dto.setId(rs.getString("id"));
        dto.setUsername(rs.getString("username"));
        dto.setFullName(rs.getString("full_name"));
        dto.setPhoneNumber(rs.getString("phone_number"));
        dto.setAddress(rs.getString("address"));
        dto.setAvatarUrl(rs.getString("avatar_url"));
        dto.setCredits(rs.getInt("credits"));
        dto.setEmail(rs.getString("email"));

        // Account status
        String accountStatus = rs.getString("account_status");
        dto.setAccountStatus(accountStatus != null ? accountStatus : "ACTIVE");
        dto.setStatusReason(rs.getString("status_reason"));

        // Last login
        java.sql.Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) {
            dto.setLastLoginAt(lastLogin.toInstant().atOffset(java.time.ZoneOffset.UTC));
        }

        // Subscription info
        String tier = rs.getString("subscription_tier");
        if (tier == null || tier.isEmpty() || tier.equals("cramerie")) {
            dto.setSubscription("FREE");
        } else {
            dto.setSubscription(tier.toUpperCase());
        }

        // Subscription dates
        java.sql.Timestamp periodStart = rs.getTimestamp("current_period_start");
        if (periodStart != null) {
            dto.setSubscriptionStart(periodStart.toInstant().atOffset(java.time.ZoneOffset.UTC));
        }
        java.sql.Timestamp periodEnd = rs.getTimestamp("current_period_end");
        if (periodEnd != null) {
            dto.setSubscriptionEnd(periodEnd.toInstant().atOffset(java.time.ZoneOffset.UTC));
        }
        dto.setAutoRenew(rs.getBoolean("auto_renew"));

        // Created at
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            dto.setCreatedAt(createdAt.toInstant().atOffset(java.time.ZoneOffset.UTC));
        }

        return dto;
    }

    /**
     * Get sort column name from sortBy parameter
     */
    private String getSortColumn(String sortBy) {
        return switch (sortBy) {
            case "username" -> "p.username";
            case "fullName" -> "p.full_name";
            case "email" -> "au.email";
            case "credits" -> "credits";
            case "subscription" -> "st.code";
            case "accountStatus" -> "p.account_status";
            case "lastLoginAt" -> "p.last_login_at";
            default -> "p.created_at";
        };
    }
}
