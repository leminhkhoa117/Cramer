package com.cramer.admin.repository;

import com.cramer.admin.domain.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link AdminAuditLog} (SPEC-17). */
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    Page<AdminAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId, Pageable pageable);

    Page<AdminAuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
