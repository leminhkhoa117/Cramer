package com.cramer.service.implement;

import com.cramer.entity.Profile;
import com.cramer.entity.SpeakingSession;
import com.cramer.repository.ProfileRepository;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.service.AdminAuditService;
import com.cramer.service.AdminSpeakingService;
import com.cramer.service.SpeakingEvaluationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminSpeakingServiceImpl implements AdminSpeakingService {

    private static final Logger logger = LoggerFactory.getLogger(AdminSpeakingServiceImpl.class);

    private final SpeakingSessionRepository speakingSessionRepository;
    private final ProfileRepository profileRepository;
    private final AdminAuditService adminAuditService;
    private final SpeakingEvaluationDispatchService speakingEvaluationService;

    public AdminSpeakingServiceImpl(SpeakingSessionRepository speakingSessionRepository,
                                     ProfileRepository profileRepository,
                                     AdminAuditService adminAuditService,
                                     SpeakingEvaluationDispatchService speakingEvaluationService) {
        this.speakingSessionRepository = speakingSessionRepository;
        this.profileRepository = profileRepository;
        this.adminAuditService = adminAuditService;
        this.speakingEvaluationService = speakingEvaluationService;
    }

    @Override
    @Transactional
    public Map<String, Object> regrade(Long sessionId, String mode, boolean force, UUID adminUserId, String reason, String ipAddress, String userAgent) {
        Profile adminProfile = requireAdmin(adminUserId);
        String adminEmail = adminProfile.getUsername();

        SpeakingSession session = speakingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Speaking session not found: " + sessionId));

        String currentStatus = session.getStatus();

        if ("graded".equals(currentStatus) && !force) {
            throw new IllegalStateException("Use force=true to regrade an already graded session");
        }
        if (!("graded".equals(currentStatus) || "grading_failed".equals(currentStatus))) {
            throw new IllegalStateException("Session status " + currentStatus + " cannot be regraded");
        }

        Map<String, Object> oldValue = new HashMap<>();
        oldValue.put("status", currentStatus);
        oldValue.put("grading_attempts", session.getGradingAttempts());
        oldValue.put("mode", mode);
        if (session.getLastGradingError() != null) {
            oldValue.put("last_grading_error", session.getLastGradingError());
        }

        session.setStatus("grading");
        session.setGradingAttempts(0);
        session.setLastGradingError(null);
        speakingSessionRepository.save(session);

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("status", "grading");
        newValue.put("grading_attempts", 0);
        newValue.put("mode", mode);

        adminAuditService.logAudit(adminUserId, adminEmail, "SPEAKING_REGRADE",
                "speaking_session", String.valueOf(sessionId),
                oldValue, newValue, reason, ipAddress, userAgent);

        logger.info("Admin {} regraded speaking session {} ({} -> grading)", adminUserId, sessionId, currentStatus);

        speakingEvaluationService.dispatchEvaluation(sessionId, session.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("newStatus", "grading");
        result.put("message", "Regrade dispatched for session " + sessionId);
        return result;
    }

    private Profile requireAdmin(UUID adminUserId) {
        Profile profile = profileRepository.findById(adminUserId)
                .orElseThrow(() -> new AccessDeniedException("Admin profile not found"));
        if (!Boolean.TRUE.equals(profile.getIsAdmin())) {
            throw new AccessDeniedException("Admin access required");
        }
        return profile;
    }
}
