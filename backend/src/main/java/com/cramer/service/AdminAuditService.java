package com.cramer.service;

import com.cramer.dto.AdminAuditLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminAuditService {
    
    // Lấy audit logs của target user
    Page<AdminAuditLogDTO> getAuditLogsForUser(String userId, Pageable pageable);
    
    // Lấy audit logs theo admin
    Page<AdminAuditLogDTO> getAuditLogsByAdmin(UUID adminUserId, Pageable pageable);
    
    // Lấy tất cả audit logs
    Page<AdminAuditLogDTO> getAllAuditLogs(Pageable pageable);
    
    // Log audit action
    void logAudit(UUID adminUserId, String adminEmail, String action, 
                  String targetType, String targetId, 
                  Map<String, Object> oldValue, Map<String, Object> newValue,
                  String description, String ipAddress, String userAgent);
    
    // Helper methods
    void logStatusChange(UUID adminUserId, String adminEmail, String userId,
                         String oldStatus, String newStatus, String reason,
                         String ipAddress);
    
    void logCreditsChange(UUID adminUserId, String adminEmail, String userId,
                          int oldBalance, int newBalance, int amount, String reason,
                          String ipAddress);
    
    void logSubscriptionChange(UUID adminUserId, String adminEmail, String userId,
                               String oldTier, String newTier, String ipAddress);
}