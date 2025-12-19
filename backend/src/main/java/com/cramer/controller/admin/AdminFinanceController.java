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
            String sql = """
                SELECT 
                    DATE(paid_at) as date,
                    SUM(amount_vnd) as total,
                    SUM(CASE WHEN type = 'SUBSCRIPTION' THEN amount_vnd ELSE 0 END) as subscriptions,
                    SUM(CASE WHEN type = 'LUA_PACK' THEN amount_vnd ELSE 0 END) as lua
                FROM public.payment_orders
                WHERE status = 'PAID' AND paid_at >= NOW() - INTERVAL '30 days'
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
     * Helper method to get date condition based on period
     */
    private String getDateCondition(String period) {
        return switch (period) {
            case "7days" -> " AND paid_at >= NOW() - INTERVAL '7 days'";
            case "30days" -> " AND paid_at >= NOW() - INTERVAL '30 days'";
            case "90days" -> " AND paid_at >= NOW() - INTERVAL '90 days'";
            case "year" -> " AND paid_at >= NOW() - INTERVAL '1 year'";
            default -> "";
        };
    }
}
