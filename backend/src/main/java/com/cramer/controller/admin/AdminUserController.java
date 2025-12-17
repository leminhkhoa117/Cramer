package com.cramer.controller.admin;

import com.cramer.dto.AdminUserDTO;
import com.cramer.dto.AdminUserListResponse;
import com.cramer.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin User Controller - Quản lý người dùng (Admin CMS)
 * 
 * API endpoints cho module Quản lý Người dùng trong Admin CMS.
 * Được bảo vệ bởi AdminAuthFilter - chỉ admin được phép truy cập.
 */
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * Lấy danh sách người dùng với phân trang và lọc
     * 
     * @param page         Số trang (mặc định 0)
     * @param size         Số dòng mỗi trang (mặc định 25)
     * @param search       Tìm kiếm theo username, email, fullName
     * @param status       Lọc theo trạng thái (ACTIVE, BANNED, DEACTIVATED)
     * @param subscription Lọc theo gói (FREE, CRAMERICH)
     * @param sortBy       Sắp xếp theo trường
     * @param sortOrder    Thứ tự sắp xếp (asc, desc)
     * @param userId       User ID từ JWT token (injected by filter)
     */
    @GetMapping
    public ResponseEntity<AdminUserListResponse> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subscription,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestHeader("X-User-Id") String userId
    ) {
        AdminUserListResponse response = adminUserService.getUsers(
                page, size, search, status, subscription, sortBy, sortOrder
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết một user
     * 
     * @param id ID của user cần xem
     * @param adminUserId User ID của admin (từ header)
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDTO> getUserById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminUserId
    ) {
        AdminUserDTO user = adminUserService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Lấy thống kê tổng quan về users
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @RequestHeader("X-User-Id") String adminUserId
    ) {
        Map<String, Object> stats = adminUserService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Cập nhật trạng thái user (Ban/Unban)
     * 
     * @param id ID của user
     * @param request Chứa status mới và lý do
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserDTO> updateUserStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") String adminUserId
    ) {
        String newStatus = request.get("status");
        String reason = request.get("reason");
        
        AdminUserDTO updatedUser = adminUserService.updateUserStatus(id, newStatus, reason, adminUserId);
        if (updatedUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Cập nhật số dư Lúa của user
     * 
     * @param id ID của user
     * @param request Chứa amount (số Lúa), action (ADD/SUBTRACT), reason
     */
    @PatchMapping("/{id}/credits")
    public ResponseEntity<AdminUserDTO> updateUserCredits(
            @PathVariable String id,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") String adminUserId
    ) {
        int amount = (Integer) request.get("amount");
        String action = (String) request.get("action");
        String reason = (String) request.get("reason");
        
        AdminUserDTO updatedUser = adminUserService.updateUserCredits(id, amount, action, reason, adminUserId);
        if (updatedUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedUser);
    }
}
