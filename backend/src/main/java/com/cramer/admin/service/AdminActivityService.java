package com.cramer.admin.service;

import com.cramer.admin.domain.AdminAuditLog;
import com.cramer.admin.repository.AdminAuditLogRepository;
import com.cramer.admin.web.dto.AdminDtos;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin audit + user-activity reads (SPEC-17 §4). Audit comes from {@code admin_audit_log} (repo);
 * user activity is a read-only projection over {@code user_activities}.
 */
@Service
@Transactional(readOnly = true)
public class AdminActivityService {

    private final AdminAuditLogRepository auditLog;
    private final JdbcTemplate jdbc;

    public AdminActivityService(AdminAuditLogRepository auditLog, JdbcTemplate jdbc) {
        this.auditLog = auditLog;
        this.jdbc = jdbc;
    }

    public List<AdminDtos.AuditLogView> auditLog(int page, int size) {
        return auditLog.findAll(PageRequest.of(Math.max(page, 0), capped(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toAudit).getContent();
    }

    public List<AdminDtos.AuditLogView> userAudit(UUID userId, int page, int size) {
        return auditLog.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("USER", userId.toString(),
                        PageRequest.of(Math.max(page, 0), capped(size)))
                .map(this::toAudit).getContent();
    }

    public List<AdminDtos.UserActivityView> userActivities(UUID userId, String type, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, activity_type, title, description, created_at
                FROM user_activities WHERE user_id = ?""");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (type != null && !type.isBlank()) {
            sql.append(" AND activity_type = ?");
            args.add(type.trim());
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        int c = capped(size);
        args.add(c);
        args.add(Math.max(page, 0) * c);
        return jdbc.query(sql.toString(), (rs, i) -> new AdminDtos.UserActivityView(
                rs.getLong("id"), rs.getString("activity_type"), rs.getString("title"),
                rs.getString("description"), rs.getObject("created_at", java.time.OffsetDateTime.class)),
                args.toArray());
    }

    public List<AdminDtos.UserActivityView> userActivitiesRecent(UUID userId, int limit) {
        return userActivities(userId, null, 0, limit);
    }

    private AdminDtos.AuditLogView toAudit(AdminAuditLog a) {
        return new AdminDtos.AuditLogView(a.getId(),
                a.getAdminUserId() == null ? null : a.getAdminUserId().toString(),
                a.getAction(), a.getTargetType(), a.getTargetId(), a.getDescription(), a.getCreatedAt());
    }

    private int capped(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
