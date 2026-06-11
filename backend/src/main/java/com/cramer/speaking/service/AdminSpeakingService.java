package com.cramer.speaking.service;

import com.cramer.admin.service.AuditPort;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.error.ResourceNotFoundException;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStateMachine;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import com.cramer.speaking.repository.SpeakingSessionRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin Speaking regrade (SPEC-14 §7), published for the admin console. <strong>Fix:</strong>
 * regrade resets the session to a <em>claimable</em> state ({@code completed}), clears
 * attempts/last-error, and writes a {@code SPEAKING_REGRADE} audit entry — so the worker (which
 * claims {@code completed}) actually re-grades. Allowed from {@code grading_failed}, or
 * {@code graded} with {@code force=true}. Dispatch is performed by the caller after commit.
 */
@Service
public class AdminSpeakingService {

    private final SpeakingSessionRepository sessions;
    private final AuditPort audit;

    public AdminSpeakingService(SpeakingSessionRepository sessions, AuditPort audit) {
        this.sessions = sessions;
        this.audit = audit;
    }

    /**
     * Reset a session to {@code completed} for regrading and audit the action.
     *
     * @return the session id (for the caller to dispatch grading post-commit)
     * @throws com.cramer.platform.error.OperationNotAllowedException if the transition is illegal
     */
    @Transactional
    public long regrade(UUID adminId, long sessionId, String mode, boolean force, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A non-blank reason is required to regrade");
        }
        SpeakingSession s = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SpeakingSession", sessionId));
        SpeakingSessionStatus from = s.getStatus();
        SpeakingSessionStatus target = SpeakingSessionStateMachine.regradeTarget(from, force);

        s.setStatus(target);
        s.setGradingAttempts(0);
        s.setLastGradingError(null);
        sessions.save(s);

        ObjectNode oldValue = Json.mapper().createObjectNode().put("status", from.name());
        ObjectNode newValue = Json.mapper().createObjectNode()
                .put("status", target.name())
                .put("mode", mode == null ? "" : mode)
                .put("force", force);
        audit.record(adminId, "SPEAKING_REGRADE", "SPEAKING_SESSION", String.valueOf(sessionId),
                reason, oldValue, newValue);
        return sessionId;
    }
}
