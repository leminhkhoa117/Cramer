package com.cramer.admin.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin read/write DTOs (SPEC-17 §2–§5). Grouped to avoid DTO sprawl; all reads are projections.
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    // ---- users ----

    public record AdminUserView(
            String id, String username, String fullName, String accountStatus,
            boolean isAdmin, String tierCode, int luaBalance, OffsetDateTime createdAt) {
    }

    public record AdminUserDetailView(
            String id, String username, String fullName, String accountStatus, String statusReason,
            boolean isAdmin, String tierCode, String subscriptionStatus, OffsetDateTime subscriptionExpiresAt,
            int luaBalance, int lifetimeEarned, int lifetimeSpent, OffsetDateTime createdAt) {
    }

    public record UserStatsView(long totalUsers, long activeUsers, long premiumUsers, long newThisMonth) {
    }

    public record StatusChangeRequest(String status, String reason) {
    }

    public record CreditAdjustRequest(int amount, String reason) {
    }

    public record SubscriptionChangeRequest(String tierCode, Integer months) {
    }

    // ---- dashboard ----

    public record DashboardStatsView(
            long totalUsers, long activeSubscriptions, long premiumSubscriptions,
            long testsTaken, long writingSubmissions, long paidOrders, long revenueVnd) {
    }

    public record SystemStatusView(String db, String payment, String ai) {
    }

    // ---- finance ----

    public record FinanceOverviewView(
            long totalRevenueVnd, long paidOrders, long subscriptionRevenueVnd, long luaRevenueVnd) {
    }

    public record RevenueBreakdownView(List<RevenueSlice> slices) {
    }

    public record RevenueSlice(String type, long revenueVnd, long orders) {
    }

    public record TopSpenderView(String userId, String username, long totalSpentVnd, long orders) {
    }

    // ---- audit / activity ----

    public record AuditLogView(
            Long id, String adminUserId, String action, String targetType, String targetId,
            String description, OffsetDateTime createdAt) {
    }

    public record UserActivityView(
            Long id, String activityType, String title, String description, OffsetDateTime createdAt) {
    }
}
