package com.cramer.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AdminFinanceController - API endpoints cho Admin Finance Dashboard
 * Cung cấp thống kê doanh thu, giao dịch và báo cáo tài chính
 */
@RestController
@RequestMapping("/api/admin/finance")
public class AdminFinanceController {

    private static final Logger log = LoggerFactory.getLogger(AdminFinanceController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get finance overview stats
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getFinanceOverview(
            @RequestParam(defaultValue = "30days") String period) {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // Determine date range based on period
            String dateCondition = getDateCondition(period);
            
            // Total revenue (PAID only)
            Long totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID'" + dateCondition,
                Long.class
            );
            overview.put("totalRevenue", totalRevenue != null ? totalRevenue : 0);
            
            // Subscription revenue
            Long subscriptionRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'" + dateCondition,
                Long.class
            );
            overview.put("subscriptionRevenue", subscriptionRevenue != null ? subscriptionRevenue : 0);
            
            // Lua revenue
            Long luaRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'LUA_PACK'" + dateCondition,
                Long.class
            );
            overview.put("luaRevenue", luaRevenue != null ? luaRevenue : 0);
            
            // New subscriptions count
            Long newSubscriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'" + dateCondition,
                Long.class
            );
            overview.put("newSubscriptions", newSubscriptions != null ? newSubscriptions : 0);
            
            // Lua packs sold
            Long luaPacksSold = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.payment_orders WHERE status = 'PAID' AND type = 'LUA_PACK'" + dateCondition,
                Long.class
            );
            overview.put("luaPacksSold", luaPacksSold != null ? luaPacksSold : 0);
            
            // Pending transactions
            Long pendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.payment_orders WHERE status = 'PENDING'",
                Long.class
            );
            overview.put("pendingTransactions", pendingCount != null ? pendingCount : 0);
            
            // Calculate changes (compare with previous period)
            overview.put("totalRevenueChange", 12.5); // Placeholder
            overview.put("subscriptionChange", 8.3);  // Placeholder
            overview.put("luaPacksChange", -2.1);     // Placeholder
            overview.put("growthRate", 15.2);         // Placeholder
            overview.put("mrr", subscriptionRevenue);
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            log.error("Error fetching finance overview", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get revenue chart data
     */
    @GetMapping("/chart")
    public ResponseEntity<List<Map<String, Object>>> getRevenueChart(
            @RequestParam(defaultValue = "30days") String period) {
        try {
            String dateCondition = getChartDateCondition(period);
            String sql = """
                SELECT 
                    DATE(paid_at) as date,
                    SUM(amount_vnd) as total,
                    SUM(CASE WHEN type = 'SUBSCRIPTION' THEN amount_vnd ELSE 0 END) as subscriptions,
                    SUM(CASE WHEN type = 'LUA_PACK' THEN amount_vnd ELSE 0 END) as lua
                FROM public.payment_orders
                WHERE status = 'PAID'
                """ + dateCondition + """
                GROUP BY DATE(paid_at)
                ORDER BY date ASC
                """;
            
            List<Map<String, Object>> chartData = jdbcTemplate.queryForList(sql);
            return ResponseEntity.ok(chartData);
            
        } catch (Exception e) {
            log.error("Error fetching revenue chart", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get revenue breakdown by type/tier
     */
    @GetMapping("/breakdown")
    public ResponseEntity<List<Map<String, Object>>> getRevenueBreakdown(
            @RequestParam(defaultValue = "30days") String period) {
        try {
            String dateCondition = getChartDateCondition(period);
            String sql = """
                SELECT 
                    COALESCE(tier_code, 'lua_pack') as name,
                    SUM(amount_vnd) as value,
                    CASE 
                        WHEN tier_code = 'cramerich' THEN '#8B5CF6'
                        WHEN tier_code = 'cramerie' THEN '#06B6D4'
                        ELSE '#F59E0B'
                    END as color
                FROM public.payment_orders
                WHERE status = 'PAID'
                """ + dateCondition + """
                GROUP BY tier_code
                ORDER BY value DESC
                """;
            
            List<Map<String, Object>> breakdown = jdbcTemplate.queryForList(sql);
            
            // Format names
            breakdown.forEach(item -> {
                String name = (String) item.get("name");
                if ("cramerich".equals(name)) {
                    item.put("name", "Cramerich");
                } else if ("cramerie".equals(name)) {
                    item.put("name", "Cramerie");
                } else {
                    item.put("name", "Gói Lúa");
                }
            });
            
            return ResponseEntity.ok(breakdown);
            
        } catch (Exception e) {
            log.error("Error fetching revenue breakdown", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get transactions list with pagination
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            
            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                whereClause.append(" AND po.status = ?");
                params.add(status);
            }
            
            if (type != null && !type.isEmpty() && !type.equals("ALL")) {
                whereClause.append(" AND po.type = ?");
                params.add(type);
            }
            
            // Count total
            String countSql = "SELECT COUNT(*) FROM public.payment_orders po" + whereClause;
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            
            // Get data with pagination
            String sql = """
                SELECT 
                    po.id,
                    po.order_code,
                    p.username,
                    po.description as product_name,
                    po.amount_vnd as amount,
                    po.type,
                    po.status,
                    po.created_at,
                    po.paid_at
                FROM public.payment_orders po
                LEFT JOIN public.profiles p ON po.user_id = p.id
                """ + whereClause + """
                 ORDER BY po.created_at DESC
                LIMIT ? OFFSET ?
                """;
            
            params.add(size);
            params.add(page * size);
            
            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql, params.toArray());
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", transactions);
            result.put("totalElements", total);
            result.put("totalPages", (int) Math.ceil((double) total / size));
            result.put("page", page);
            result.put("size", size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error fetching transactions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get top spenders
     */
    @GetMapping("/top-spenders")
    public ResponseEntity<List<Map<String, Object>>> getTopSpenders(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            String sql = """
                SELECT 
                    p.id as user_id,
                    p.username,
                    p.full_name,
                    SUM(po.amount_vnd) as total_spent
                FROM public.payment_orders po
                JOIN public.profiles p ON po.user_id = p.id
                WHERE po.status = 'PAID'
                GROUP BY p.id, p.username, p.full_name
                ORDER BY total_spent DESC
                LIMIT ?
                """;
            
            List<Map<String, Object>> topSpenders = jdbcTemplate.queryForList(sql, limit);
            
            // Add rank
            for (int i = 0; i < topSpenders.size(); i++) {
                topSpenders.get(i).put("rank", i + 1);
            }
            
            return ResponseEntity.ok(topSpenders);
            
        } catch (Exception e) {
            log.error("Error fetching top spenders", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Get export data (for Excel export)
     */
    @GetMapping("/export")
    public ResponseEntity<List<Map<String, Object>>> getExportData(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String status) {
        try {
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            
            if (dateFrom != null && !dateFrom.isEmpty()) {
                whereClause.append(" AND po.created_at >= ?::timestamp");
                params.add(dateFrom);
            }
            
            if (dateTo != null && !dateTo.isEmpty()) {
                whereClause.append(" AND po.created_at <= ?::timestamp");
                params.add(dateTo);
            }
            
            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                whereClause.append(" AND po.status = ?");
                params.add(status);
            }
            
            String sql = """
                SELECT 
                    po.order_code as "Mã đơn hàng",
                    p.username as "Tên người dùng",
                    p.full_name as "Họ tên",
                    po.description as "Sản phẩm",
                    po.type as "Loại",
                    po.amount_vnd as "Số tiền (VNĐ)",
                    po.status as "Trạng thái",
                    po.created_at as "Ngày tạo",
                    po.paid_at as "Ngày thanh toán"
                FROM public.payment_orders po
                LEFT JOIN public.profiles p ON po.user_id = p.id
                """ + whereClause + """
                 ORDER BY po.created_at DESC
                """;
            
            List<Map<String, Object>> exportData = jdbcTemplate.queryForList(sql, params.toArray());
            return ResponseEntity.ok(exportData);
            
        } catch (Exception e) {
            log.error("Error fetching export data", e);
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
    
    /**
     * Helper method to get date condition based on period (with AND prefix for overview queries)
     */
    private String getDateCondition(String period) {
        return switch (period) {
            case "today" -> " AND paid_at >= DATE_TRUNC('day', NOW())";
            case "yesterday" -> " AND paid_at >= DATE_TRUNC('day', NOW() - INTERVAL '1 day') AND paid_at < DATE_TRUNC('day', NOW())";
            case "7days" -> " AND paid_at >= NOW() - INTERVAL '7 days'";
            case "30days" -> " AND paid_at >= NOW() - INTERVAL '30 days'";
            case "thisMonth" -> " AND paid_at >= DATE_TRUNC('month', NOW())";
            case "lastMonth" -> " AND paid_at >= DATE_TRUNC('month', NOW() - INTERVAL '1 month') AND paid_at < DATE_TRUNC('month', NOW())";
            case "thisYear" -> " AND paid_at >= DATE_TRUNC('year', NOW())";
            case "90days" -> " AND paid_at >= NOW() - INTERVAL '90 days'";
            case "year" -> " AND paid_at >= NOW() - INTERVAL '1 year'";
            default -> " AND paid_at >= NOW() - INTERVAL '30 days'";
        };
    }
    
    /**
     * Helper method to get date condition for chart queries (requires AND prefix)
     */
    private String getChartDateCondition(String period) {
        return switch (period) {
            case "today" -> " AND paid_at >= DATE_TRUNC('day', NOW())";
            case "yesterday" -> " AND paid_at >= DATE_TRUNC('day', NOW() - INTERVAL '1 day') AND paid_at < DATE_TRUNC('day', NOW())";
            case "7days" -> " AND paid_at >= NOW() - INTERVAL '7 days'";
            case "30days" -> " AND paid_at >= NOW() - INTERVAL '30 days'";
            case "thisMonth" -> " AND paid_at >= DATE_TRUNC('month', NOW())";
            case "lastMonth" -> " AND paid_at >= DATE_TRUNC('month', NOW() - INTERVAL '1 month') AND paid_at < DATE_TRUNC('month', NOW())";
            case "thisYear" -> " AND paid_at >= DATE_TRUNC('year', NOW())";
            case "90days" -> " AND paid_at >= NOW() - INTERVAL '90 days'";
            case "year" -> " AND paid_at >= NOW() - INTERVAL '1 year'";
            default -> " AND paid_at >= NOW() - INTERVAL '30 days'";
        };
    }

    /**
     * Get reports data with custom date range
     */
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> getReportsData(
            @RequestParam String dateFrom,
            @RequestParam String dateTo,
            @RequestParam(defaultValue = "daily") String granularity) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Build date condition
            String dateCondition = " AND paid_at >= ?::timestamp AND paid_at < (?::timestamp + INTERVAL '1 day')";
            
            // Get overview for current period
            Map<String, Object> overview = new HashMap<>();
            Long totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID'" + dateCondition,
                Long.class, dateFrom, dateTo
            );
            overview.put("totalRevenue", totalRevenue != null ? totalRevenue : 0);
            
            Long subscriptionRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'" + dateCondition,
                Long.class, dateFrom, dateTo
            );
            overview.put("subscriptionRevenue", subscriptionRevenue != null ? subscriptionRevenue : 0);
            
            Long luaRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'LUA_PACK'" + dateCondition,
                Long.class, dateFrom, dateTo
            );
            overview.put("luaRevenue", luaRevenue != null ? luaRevenue : 0);
            
            // Calculate days in period
            java.time.LocalDate fromDate = java.time.LocalDate.parse(dateFrom);
            java.time.LocalDate toDate = java.time.LocalDate.parse(dateTo);
            long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            
            // Get previous period data
            java.time.LocalDate prevToDate = fromDate.minusDays(1);
            java.time.LocalDate prevFromDate = prevToDate.minusDays(daysInPeriod - 1);
            
            Long prevTotalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID'" + dateCondition,
                Long.class, prevFromDate.toString(), prevToDate.toString()
            );
            
            // Calculate changes
            double totalChange = prevTotalRevenue != null && prevTotalRevenue > 0 
                ? ((totalRevenue - prevTotalRevenue) * 100.0 / prevTotalRevenue) : 0;
            overview.put("totalRevenueChange", Math.round(totalChange * 10) / 10.0);
            
            Long prevSubRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'" + dateCondition,
                Long.class, prevFromDate.toString(), prevToDate.toString()
            );
            double subChange = prevSubRevenue != null && prevSubRevenue > 0 
                ? ((subscriptionRevenue - prevSubRevenue) * 100.0 / prevSubRevenue) : 0;
            overview.put("subscriptionChange", Math.round(subChange * 10) / 10.0);
            
            Long prevLuaRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'LUA_PACK'" + dateCondition,
                Long.class, prevFromDate.toString(), prevToDate.toString()
            );
            double luaChange = prevLuaRevenue != null && prevLuaRevenue > 0 
                ? ((luaRevenue - prevLuaRevenue) * 100.0 / prevLuaRevenue) : 0;
            overview.put("luaPacksChange", Math.round(luaChange * 10) / 10.0);
            overview.put("growthRate", overview.get("totalRevenueChange"));
            
            result.put("overview", overview);
            
            // Period comparison
            Map<String, Object> comparison = new HashMap<>();
            comparison.put("currentPeriod", Map.of(
                "from", dateFrom,
                "to", dateTo,
                "revenue", totalRevenue
            ));
            comparison.put("previousPeriod", Map.of(
                "from", prevFromDate.toString(),
                "to", prevToDate.toString(),
                "revenue", prevTotalRevenue != null ? prevTotalRevenue : 0
            ));
            long diff = totalRevenue - (prevTotalRevenue != null ? prevTotalRevenue : 0);
            comparison.put("difference", diff);
            comparison.put("changePercent", overview.get("totalRevenueChange"));
            result.put("comparison", comparison);
            
            // Get chart data based on granularity
            String groupBy = switch (granularity) {
                case "weekly" -> "DATE_TRUNC('week', paid_at)";
                case "monthly" -> "DATE_TRUNC('month', paid_at)";
                default -> "DATE(paid_at)";
            };
            
            String chartSql = """
                SELECT 
                    %s as date,
                    SUM(amount_vnd) as total,
                    SUM(CASE WHEN type = 'SUBSCRIPTION' THEN amount_vnd ELSE 0 END) as subscriptions,
                    SUM(CASE WHEN type = 'LUA_PACK' THEN amount_vnd ELSE 0 END) as lua
                FROM public.payment_orders
                WHERE status = 'PAID' AND paid_at >= ?::timestamp AND paid_at < (?::timestamp + INTERVAL '1 day')
                GROUP BY %s
                ORDER BY date ASC
                """.formatted(groupBy, groupBy);
            
            List<Map<String, Object>> chartData = jdbcTemplate.queryForList(chartSql, dateFrom, dateTo);
            result.put("chartData", chartData);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error fetching reports data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get subscription analysis data
     */
    @GetMapping("/reports/subscriptions")
    public ResponseEntity<Map<String, Object>> getSubscriptionAnalysis(
            @RequestParam String dateFrom,
            @RequestParam String dateTo) {
        try {
            Map<String, Object> result = new HashMap<>();
            String dateCondition = " AND paid_at >= ?::timestamp AND paid_at < (?::timestamp + INTERVAL '1 day')";
            
            // MRR (Monthly Recurring Revenue) - based on active subscriptions
            Long activeSubscribers = 0L;
            try {
                activeSubscribers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM public.user_subscriptions WHERE status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > NOW())",
                    Long.class
                );
            } catch (Exception e) {
                log.warn("Could not fetch active subscribers: {}", e.getMessage());
            }
            result.put("activeSubscribers", activeSubscribers != null ? activeSubscribers : 0);
            
            // Calculate MRR from subscription prices
            Long mrr = 0L;
            try {
                mrr = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(SUM(
                        CASE 
                            WHEN st.code = 'cramerich' THEN st.price_vnd
                            WHEN st.code = 'cramerie' THEN st.price_vnd
                            ELSE 0
                        END
                    ), 0)
                    FROM public.user_subscriptions us
                    JOIN public.subscription_tiers st ON us.tier_id = st.id
                    WHERE us.status = 'ACTIVE' AND (us.expires_at IS NULL OR us.expires_at > NOW())
                    """,
                    Long.class
                );
            } catch (Exception e) {
                log.warn("Could not calculate MRR: {}", e.getMessage());
            }
            result.put("mrr", mrr != null ? mrr : 0);
            
            // New subscribers in period
            Long newSubscribers = 0L;
            try {
                newSubscribers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'" + dateCondition,
                    Long.class, dateFrom, dateTo
                );
            } catch (Exception e) {
                log.warn("Could not fetch new subscribers: {}", e.getMessage());
            }
            result.put("newSubscribers", newSubscribers != null ? newSubscribers : 0);
            
            // Cancelled/expired subscribers 
            Long cancelledSubscribers = 0L;
            try {
                cancelledSubscribers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.user_subscriptions WHERE status IN ('EXPIRED', 'CANCELLED') AND expires_at >= ?::timestamp AND expires_at < (?::timestamp + INTERVAL '1 day')",
                    Long.class, dateFrom, dateTo
                );
            } catch (Exception e) {
                log.warn("Could not fetch cancelled subscribers: {}", e.getMessage());
            }
            result.put("cancelledSubscribers", cancelledSubscribers != null ? cancelledSubscribers : 0);
            
            // Calculate churn rate
            double churnRate = activeSubscribers != null && activeSubscribers > 0
                ? ((cancelledSubscribers != null ? cancelledSubscribers : 0) * 100.0 / activeSubscribers) : 0;
            result.put("churnRate", Math.round(churnRate * 10) / 10.0);
            
            // LTV (Lifetime Value) - average revenue per subscriber
            Long totalSubRevenue = 0L;
            Long totalSubCount = 0L;
            try {
                totalSubRevenue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount_vnd), 0) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'",
                    Long.class
                );
                totalSubCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION'",
                    Long.class
                );
            } catch (Exception e) {
                log.warn("Could not calculate LTV: {}", e.getMessage());
            }
            Long ltv = (totalSubCount != null && totalSubCount > 0) 
                ? (totalSubRevenue / totalSubCount) : 0;
            result.put("ltv", ltv);
            
            // Changes (placeholder - would need previous period data)
            result.put("mrrChange", 8.5);
            result.put("churnRateChange", -0.3);
            result.put("ltvChange", 5.2);
            
            // Cohort data (simplified)
            List<Map<String, Object>> cohortData = List.of(
                Map.of("cohort", "Tháng 10", "month1", 100, "month2", 85, "month3", 72),
                Map.of("cohort", "Tháng 11", "month1", 100, "month2", 88),
                Map.of("cohort", "Tháng 12", "month1", 100)
            );
            result.put("cohortData", cohortData);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error fetching subscription analysis", e);
            // Return default data instead of error
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("activeSubscribers", 0);
            fallback.put("mrr", 0);
            fallback.put("newSubscribers", 0);
            fallback.put("cancelledSubscribers", 0);
            fallback.put("churnRate", 0);
            fallback.put("ltv", 0);
            fallback.put("mrrChange", 0);
            fallback.put("churnRateChange", 0);
            fallback.put("ltvChange", 0);
            fallback.put("cohortData", List.of());
            return ResponseEntity.ok(fallback);
        }
    }

    /**
     * Get Lua economy data
     */
    @GetMapping("/reports/lua-economy")
    public ResponseEntity<Map<String, Object>> getLuaEconomy(
            @RequestParam String dateFrom,
            @RequestParam String dateTo) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Try to get data from credit_transactions - use 'amount' column which is more standard
            Long totalPurchased = 0L;
            Long bonusLua = 0L;
            Long totalSpent = 0L;
            
            try {
                // Total earned (purchases) - positive amounts with EARN type
                totalPurchased = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(CASE WHEN amount > 0 AND category = 'PURCHASE' THEN amount ELSE 0 END), 0) FROM public.credit_transactions",
                    Long.class
                );
                
                // Bonus Lua - BONUS category
                bonusLua = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(CASE WHEN amount > 0 AND (category = 'BONUS' OR category = 'SUBSCRIPTION_BONUS' OR category = 'INITIAL') THEN amount ELSE 0 END), 0) FROM public.credit_transactions",
                    Long.class
                );
                
                // Total spent - negative amounts
                totalSpent = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(ABS(amount)), 0) FROM public.credit_transactions WHERE amount < 0",
                    Long.class
                );
            } catch (Exception e) {
                log.warn("Could not fetch from credit_transactions: {}", e.getMessage());
            }
            
            result.put("purchasedLua", totalPurchased != null ? totalPurchased : 0);
            result.put("bonusLua", bonusLua != null ? bonusLua : 0);
            
            long totalIssued = (totalPurchased != null ? totalPurchased : 0) + (bonusLua != null ? bonusLua : 0);
            result.put("totalIssued", totalIssued);
            result.put("totalSpent", totalSpent != null ? totalSpent : 0);
            
            // In circulation - from user_credits table
            Long inCirculation = 0L;
            Double avgBalance = 0.0;
            try {
                inCirculation = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(balance), 0) FROM public.user_credits",
                    Long.class
                );
                avgBalance = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(AVG(balance), 0) FROM public.user_credits WHERE balance > 0",
                    Double.class
                );
            } catch (Exception e) {
                log.warn("Could not fetch from user_credits, trying profiles: {}", e.getMessage());
                // Fallback to profiles.credits if user_credits doesn't exist
                try {
                    inCirculation = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(credits), 0) FROM public.profiles WHERE credits IS NOT NULL",
                        Long.class
                    );
                    avgBalance = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(AVG(credits), 0) FROM public.profiles WHERE credits > 0",
                        Double.class
                    );
                } catch (Exception e2) {
                    log.warn("Could not fetch credits from profiles either: {}", e2.getMessage());
                }
            }
            result.put("inCirculation", inCirculation != null ? inCirculation : 0);
            result.put("avgBalance", avgBalance != null ? Math.round(avgBalance) : 0);
            
            // Top features spending Lua - try to get from actual data
            List<Map<String, Object>> topFeatures;
            try {
                topFeatures = jdbcTemplate.queryForList(
                    """
                    SELECT 
                        category as name,
                        SUM(ABS(amount)) as spent
                    FROM public.credit_transactions 
                    WHERE amount < 0 AND type = 'SPEND'
                    GROUP BY category
                    ORDER BY spent DESC
                    LIMIT 4
                    """
                );
                // Calculate percentages
                long totalSpentCalc = topFeatures.stream()
                    .mapToLong(f -> ((Number) f.get("spent")).longValue())
                    .sum();
                for (Map<String, Object> feature : topFeatures) {
                    long spent = ((Number) feature.get("spent")).longValue();
                    int percentage = totalSpentCalc > 0 ? (int) (spent * 100 / totalSpentCalc) : 0;
                    feature.put("percentage", percentage);
                    // Format name
                    String name = (String) feature.get("name");
                    feature.put("name", formatCategoryName(name));
                }
            } catch (Exception e) {
                log.warn("Could not fetch top features: {}", e.getMessage());
                // Fallback with sample data
                topFeatures = List.of(
                    Map.of("name", "AI Essay Review", "spent", 0, "percentage", 0),
                    Map.of("name", "Speaking Practice", "spent", 0, "percentage", 0),
                    Map.of("name", "Extra Tests", "spent", 0, "percentage", 0),
                    Map.of("name", "Other", "spent", 0, "percentage", 0)
                );
            }
            result.put("topFeatures", topFeatures);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error fetching lua economy", e);
            // Return default fallback data
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("purchasedLua", 0);
            fallback.put("bonusLua", 0);
            fallback.put("totalIssued", 0);
            fallback.put("totalSpent", 0);
            fallback.put("inCirculation", 0);
            fallback.put("avgBalance", 0);
            fallback.put("topFeatures", List.of());
            return ResponseEntity.ok(fallback);
        }
    }
    
    private String formatCategoryName(String category) {
        if (category == null) return "Other";
        return switch (category.toUpperCase()) {
            case "AI_GRADING" -> "AI Essay Review";
            case "SPEAKING_PRACTICE" -> "Speaking Practice";
            case "EXTRA_ATTEMPT" -> "Extra Tests";
            case "CHATBOT" -> "Chatbot Usage";
            case "TRANSLATION" -> "AI Translation";
            default -> category;
        };
    }

    /**
     * Get user acquisition data
     */
    @GetMapping("/reports/acquisition")
    public ResponseEntity<Map<String, Object>> getUserAcquisition(
            @RequestParam String dateFrom,
            @RequestParam String dateTo) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Total users from profiles
            Long totalUsers = 0L;
            Long paidUsers = 0L;
            
            try {
                totalUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM public.profiles",
                    Long.class
                );
            } catch (Exception e) {
                log.warn("Could not count profiles: {}", e.getMessage());
            }
            
            try {
                paidUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM public.user_subscriptions WHERE status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > NOW())",
                    Long.class
                );
            } catch (Exception e) {
                log.warn("Could not count paid users: {}", e.getMessage());
            }
            
            long freeUsers = (totalUsers != null ? totalUsers : 0) - (paidUsers != null ? paidUsers : 0);
            result.put("freeUsers", Math.max(0, freeUsers));
            result.put("paidUsers", paidUsers != null ? paidUsers : 0);
            
            // Conversion rate
            double conversionRate = totalUsers != null && totalUsers > 0 
                ? ((paidUsers != null ? paidUsers : 0) * 100.0 / totalUsers) : 0;
            result.put("conversionRate", Math.round(conversionRate * 10) / 10.0);
            
            // New paid conversions in period
            Long convertedThisPeriod = 0L;
            try {
                convertedThisPeriod = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM public.payment_orders WHERE status = 'PAID' AND type = 'SUBSCRIPTION' AND created_at >= ?::timestamp AND created_at < (?::timestamp + INTERVAL '1 day')",
                    Long.class, dateFrom, dateTo
                );
            } catch (Exception e) {
                log.warn("Could not count conversions: {}", e.getMessage());
            }
            result.put("convertedThisPeriod", convertedThisPeriod != null ? convertedThisPeriod : 0);
            
            // Bonus cost (Lua given as bonus)
            Long bonusCost = 0L;
            try {
                bonusCost = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) * 100 FROM public.credit_transactions WHERE type = 'EARN' AND category IN ('BONUS', 'INITIAL') AND created_at >= ?::timestamp AND created_at < (?::timestamp + INTERVAL '1 day')",
                    Long.class, dateFrom, dateTo
                );
            } catch (Exception e) {
                log.warn("Could not calculate bonus cost: {}", e.getMessage());
            }
            result.put("bonusCost", bonusCost != null ? bonusCost : 0);
            
            // Placeholder values
            result.put("avgTimeToConvert", 14);
            result.put("conversionRateChange", 2.5);
            result.put("promotionROI", 320);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error fetching user acquisition", e);
            // Return default fallback data
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("freeUsers", 0);
            fallback.put("paidUsers", 0);
            fallback.put("conversionRate", 0);
            fallback.put("convertedThisPeriod", 0);
            fallback.put("bonusCost", 0);
            fallback.put("avgTimeToConvert", 0);
            fallback.put("conversionRateChange", 0);
            fallback.put("promotionROI", 0);
            return ResponseEntity.ok(fallback);
        }
    }
}
