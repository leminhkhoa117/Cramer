package com.cramer.admin.web;

import com.cramer.admin.service.AdminUserService;
import com.cramer.admin.web.dto.AdminDtos;
import com.cramer.platform.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin user management (SPEC-17 §2). Admin-gated by the security chain; admin id is the
 * authenticated principal (no {@code X-User-Id}).
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService users;
    private final CurrentUser currentUser;

    public AdminUserController(AdminUserService users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<AdminDtos.AdminUserView> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "created_at") String sortBy,
                                              @RequestParam(defaultValue = "desc") String sortOrder,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return users.listUsers(search, status, sortBy, sortOrder, page, size);
    }

    @GetMapping("/stats")
    public AdminDtos.UserStatsView stats() {
        return users.userStats();
    }

    @GetMapping("/{id}")
    public AdminDtos.AdminUserDetailView detail(@PathVariable UUID id) {
        return users.userDetail(id);
    }

    @PatchMapping("/{id}/status")
    public Map<String, Object> setStatus(@PathVariable UUID id,
                                         @RequestBody AdminDtos.StatusChangeRequest request) {
        users.setStatus(currentUser.requireUserId(), id, request.status(), request.reason());
        return Map.of("success", true);
    }

    @PatchMapping("/{id}/credits")
    public Map<String, Object> adjustCredits(@PathVariable UUID id,
                                             @RequestBody AdminDtos.CreditAdjustRequest request) {
        int balance = users.adjustCredits(currentUser.requireUserId(), id, request.amount(), request.reason());
        return Map.of("success", true, "balance", balance);
    }

    @PatchMapping("/{id}/subscription")
    public Map<String, Object> setSubscription(@PathVariable UUID id,
                                               @RequestBody AdminDtos.SubscriptionChangeRequest request) {
        int months = request.months() == null ? 1 : request.months();
        users.setSubscription(currentUser.requireUserId(), id, request.tierCode(), months);
        return Map.of("success", true);
    }
}
