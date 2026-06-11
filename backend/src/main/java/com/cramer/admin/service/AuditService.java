package com.cramer.admin.service;

import com.cramer.admin.domain.AdminAuditLog;
import com.cramer.admin.repository.AdminAuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implements {@link AuditPort} (SPEC-17): writes {@code admin_audit_log}. The owning module of the
 * audit trail, so other modules log admin actions through this port.
 */
@Service
public class AuditService implements AuditPort {

    private final AdminAuditLogRepository auditLog;

    public AuditService(AdminAuditLogRepository auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public void record(UUID adminUserId, String action, String targetType, String targetId,
                       String description, JsonNode oldValue, JsonNode newValue) {
        AdminAuditLog row = new AdminAuditLog();
        row.setAdminUserId(adminUserId);
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setDescription(description);
        row.setOldValue(oldValue);
        row.setNewValue(newValue);
        auditLog.save(row);
    }
}
