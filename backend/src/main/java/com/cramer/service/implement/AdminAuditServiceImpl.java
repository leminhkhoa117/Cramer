package com.cramer.service.implement;

import com.cramer.dto.AdminAuditLogDTO;
import com.cramer.entity.AdminAuditLog;
import com.cramer.repository.AdminAuditLogRepository;
import com.cramer.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditServiceImpl implements AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    @Override
    public Page<AdminAuditLogDTO> getAuditLogsForUser(String userId, Pageable pageable) {
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                AdminAuditLog.TARGET_USER, userId, pageable)
                .map(this::toDTO);
    }

    @Override
    public Page<AdminAuditLogDTO> getAuditLogsByAdmin(UUID adminUserId, Pageable pageable) {
        return auditLogRepository.findByAdminUserIdOrderByCreatedAtDesc(adminUserId, pageable)
                .map(this::toDTO);
    }

    @Override
    public Page<AdminAuditLogDTO> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(Objects.requireNonNull(pageable)).map(this::toDTO);
    }

    @Override
    @Transactional
    public void logAudit(UUID adminUserId, String adminEmail, String action,
            String targetType, String targetId,
            Map<String, Object> oldValue, Map<String, Object> newValue,
            String description, String ipAddress, String userAgent) {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAdminUserId(adminUserId);
        auditLog.setAdminEmail(adminEmail);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setDescription(description);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreatedAt(OffsetDateTime.now());

        auditLogRepository.save(auditLog);
        log.info("Admin audit: {} performed {} on {} {}", adminEmail, action, targetType, targetId);
    }

    @Override
    public void logStatusChange(UUID adminUserId, String adminEmail, String userId,
            String oldStatus, String newStatus, String reason,
            String ipAddress) {
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("status", oldStatus);

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("status", newStatus);
        newValue.put("reason", reason);

        String action = "BANNED".equals(newStatus) ? AdminAuditLog.ACTION_BAN
                : "ACTIVE".equals(newStatus) ? AdminAuditLog.ACTION_UNBAN : AdminAuditLog.ACTION_STATUS_CHANGE;

        String description = String.format("Đổi trạng thái từ %s sang %s. Lý do: %s",
                oldStatus, newStatus, reason != null ? reason : "Không có");

        logAudit(adminUserId, adminEmail, action, AdminAuditLog.TARGET_USER, userId,
                oldValue, newValue, description, ipAddress, null);
    }

    @Override
    public void logCreditsChange(UUID adminUserId, String adminEmail, String userId,
            int oldBalance, int newBalance, int amount, String reason,
            String ipAddress) {
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("balance", oldBalance);

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("balance", newBalance);
        newValue.put("amount", amount);
        newValue.put("reason", reason);

        String action = amount > 0 ? AdminAuditLog.ACTION_CREDITS_ADD : AdminAuditLog.ACTION_CREDITS_SUBTRACT;
        String description = String.format("%s %d Lúa. Số dư: %d → %d. Lý do: %s",
                amount > 0 ? "Thêm" : "Trừ", Math.abs(amount), oldBalance, newBalance, reason);

        logAudit(adminUserId, adminEmail, action, AdminAuditLog.TARGET_CREDITS, userId,
                oldValue, newValue, description, ipAddress, null);
    }

    @Override
    public void logSubscriptionChange(UUID adminUserId, String adminEmail, String userId,
            String oldTier, String newTier, String ipAddress) {
        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("tier", oldTier);

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("tier", newTier);

        String description = String.format("Đổi gói từ %s sang %s", oldTier, newTier);

        logAudit(adminUserId, adminEmail, AdminAuditLog.ACTION_SUBSCRIPTION_CHANGE,
                AdminAuditLog.TARGET_SUBSCRIPTION, userId, oldValue, newValue, description, ipAddress, null);
    }

    private AdminAuditLogDTO toDTO(AdminAuditLog log) {
        AdminAuditLogDTO dto = new AdminAuditLogDTO();
        dto.setId(log.getId());
        dto.setAdminUserId(log.getAdminUserId());
        dto.setAdminEmail(log.getAdminEmail());
        dto.setAction(log.getAction());
        dto.setTargetType(log.getTargetType());
        dto.setTargetId(log.getTargetId());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setDescription(log.getDescription());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}