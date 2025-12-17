package com.cramer.service.implement;

import com.cramer.dto.AdminUserDTO;
import com.cramer.dto.AdminUserListResponse;
import com.cramer.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Admin User Service Implementation - Xử lý logic quản lý users cho Admin CMS
 * 
 * Sử dụng JdbcTemplate để query trực tiếp từ database Supabase.
 * Kết hợp dữ liệu từ nhiều bảng: profiles, user_credits, user_subscriptions, subscription_tiers, auth.users.
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
            
            long totalItems = jdbcTemplate.queryForObject(countBuilder.toString(), Long.class, countParams.toArray());
            
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
                sqlBuilder.toString(),
                params.toArray(),
                this::mapRowToAdminUserDTO
            );
            
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
            
            List<AdminUserDTO> results = jdbcTemplate.query(sql, new Object[]{id}, (rs, rowNum) -> {
                AdminUserDTO dto = mapRowToAdminUserDTO(rs, rowNum);
                dto.setTotalTests(rs.getInt("total_tests"));
                dto.setTotalVocabulary(rs.getInt("total_vocabulary"));
                return dto;
            });
            
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
                "SELECT COUNT(*) FROM public.profiles", Long.class
            );
            stats.put("totalUsers", totalUsers);
            
            // Active users (users who have test attempts in last 30 days)
            Long activeUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM public.test_attempts WHERE created_at > NOW() - INTERVAL '30 days'",
                Long.class
            );
            stats.put("activeUsers", activeUsers);
            
            // Premium users (Cramerich with active status)
            Long premiumUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.user_subscriptions us " +
                "JOIN public.subscription_tiers st ON us.tier_id = st.id " +
                "WHERE st.code = 'cramerich' AND us.status = 'active'",
                Long.class
            );
            stats.put("premiumUsers", premiumUsers);
            
            // New users this month
            Long newUsersThisMonth = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.profiles WHERE created_at >= DATE_TRUNC('month', NOW())",
                Long.class
            );
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
            jdbcTemplate.update(
                "UPDATE public.profiles SET account_status = ?, status_reason = ? WHERE id = ?::uuid",
                newStatus, reason, userId
            );
            
            logger.info("Admin {} updated user {} status to {} with reason: {}", adminId, userId, newStatus, reason);
            
            return getUserById(userId);
        } catch (Exception e) {
            logger.error("Error updating user status: " + userId, e);
            return getUserById(userId);
        }
    }
    
    @Override
    public AdminUserDTO updateUserCredits(String userId, int amount, String action, String reason, String adminId) {
        try {
            // Get current balance
            Integer currentBalance = null;
            try {
                currentBalance = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(balance, 0) FROM public.user_credits WHERE user_id = ?::uuid",
                    Integer.class,
                    userId
                );
            } catch (Exception e) {
                currentBalance = 0;
            }
            
            int newBalance;
            if ("ADD".equalsIgnoreCase(action)) {
                newBalance = (currentBalance != null ? currentBalance : 0) + amount;
            } else {
                newBalance = Math.max(0, (currentBalance != null ? currentBalance : 0) - amount);
            }
            
            // Update or insert credits
            int updated = jdbcTemplate.update(
                "UPDATE public.user_credits SET balance = ?, updated_at = NOW() WHERE user_id = ?::uuid",
                newBalance, userId
            );
            
            if (updated == 0) {
                // Insert new record
                jdbcTemplate.update(
                    "INSERT INTO public.user_credits (user_id, balance, lifetime_earned, lifetime_spent, created_at, updated_at) " +
                    "VALUES (?::uuid, ?, 0, 0, NOW(), NOW())",
                    userId, newBalance
                );
            }
            
            // Log the credit change
            try {
                jdbcTemplate.update(
                    "INSERT INTO public.credit_transactions (user_id, amount, type, category, description, created_at) " +
                    "VALUES (?::uuid, ?, 'BONUS', 'ADMIN_ADJUSTMENT', ?, NOW())",
                    userId,
                    "ADD".equalsIgnoreCase(action) ? amount : -amount,
                    "Admin: " + reason
                );
            } catch (Exception e) {
                logger.warn("Could not log credit transaction: " + e.getMessage());
            }
            
            logger.info("Admin {} {} {} Lúa for user {} with reason: {}", 
                adminId, action, amount, userId, reason);
            
            return getUserById(userId);
            
        } catch (Exception e) {
            logger.error("Error updating credits for user: " + userId, e);
            return getUserById(userId);
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
