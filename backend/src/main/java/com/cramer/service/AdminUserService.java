package com.cramer.service;

import com.cramer.dto.AdminUserDTO;
import com.cramer.dto.AdminUserListResponse;

import java.util.Map;

/**
 * Admin User Service Interface - Xử lý logic quản lý users cho Admin CMS
 */
public interface AdminUserService {
    
    /**
     * Lấy danh sách users với phân trang và lọc
     */
    AdminUserListResponse getUsers(int page, int size, String search, String status, 
                                    String subscription, String sortBy, String sortOrder);
    
    /**
     * Lấy chi tiết một user theo ID
     */
    AdminUserDTO getUserById(String id);
    
    /**
     * Lấy thống kê tổng quan về users
     */
    Map<String, Object> getUserStats();
    
    /**
     * Cập nhật trạng thái user
     */
    AdminUserDTO updateUserStatus(String userId, String newStatus, String reason, String adminId);
    
    /**
     * Cập nhật số dư Lúa của user
     */
    AdminUserDTO updateUserCredits(String userId, int amount, String action, String reason, String adminId);
    
    /**
     * Cập nhật gói đăng ký của user (Admin thay đổi subscription tier)
     * 
     * @param userId ID của user cần thay đổi gói
     * @param newTierCode Mã gói mới (cramerie, cramerich)
     * @param durationMonths Thời hạn gói (1, 3, 6 tháng). Chỉ áp dụng cho gói trả phí.
     * @param reason Lý do thay đổi
     * @param adminId ID của admin thực hiện thay đổi
     * @return AdminUserDTO với thông tin cập nhật
     */
    AdminUserDTO updateUserSubscription(String userId, String newTierCode, int durationMonths, String reason, String adminId);
}
