package com.cramer.speaking.grading;

import com.cramer.billing.service.SpeakingBillingPort;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStateMachine;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import com.cramer.speaking.repository.SpeakingSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Short transactional boundaries for the Speaking grading lifecycle (SPEC-14 §6). Each method
 * runs in its own transaction with a row lock so the slow OpenRouter call in
 * {@link SpeakingGradingWorker} happens <em>outside</em> any DB transaction/lock. Claiming
 * transitions {@code completed → grading}; finishing persists bands or fails+refunds.
 */
@Service
public class SpeakingGradingStore {

    private static final Logger log = LoggerFactory.getLogger(SpeakingGradingStore.class);

    private final SpeakingSessionRepository sessions;
    private final SpeakingBillingPort billing;

    public SpeakingGradingStore(SpeakingSessionRepository sessions, SpeakingBillingPort billing) {
        this.sessions = sessions;
        this.billing = billing;
    }

    /** Claim a session for grading: {@code completed → grading} under a lock. False if not claimable. */
    @Transactional
    public boolean claim(long sessionId) {
        SpeakingSession s = sessions.findByIdForUpdate(sessionId).orElse(null);
        if (s == null || s.getStatus() != SpeakingSessionStatus.COMPLETED) {
            return false;
        }
        SpeakingSessionStateMachine.requireTransition(s.getStatus(), SpeakingSessionStatus.GRADING);
        s.setStatus(SpeakingSessionStatus.GRADING);
        s.setGradingAttempts(s.getGradingAttempts() == null ? 1 : s.getGradingAttempts() + 1);
        sessions.save(s);
        return true;
    }

    @Transactional(readOnly = true)
    public SpeakingSession load(long sessionId) {
        return sessions.findById(sessionId).orElse(null);
    }

    /** Persist a successful grading: bands + full result JSON; {@code grading → graded}. */
    @Transactional
    public void finishSuccess(long sessionId, SpeakingGradingResult result, JsonNode rawResult) {
        SpeakingSession s = sessions.findByIdForUpdate(sessionId).orElse(null);
        if (s == null || s.getStatus() != SpeakingSessionStatus.GRADING) {
            return;
        }
        SpeakingSessionStateMachine.requireTransition(s.getStatus(), SpeakingSessionStatus.GRADED);
        s.setStatus(SpeakingSessionStatus.GRADED);
        s.setOverallBand(band(result.overallBand()));
        s.setFluencyBand(band(result.fluencyBand()));
        s.setLexicalBand(band(result.lexicalBand()));
        s.setGrammarBand(band(result.grammarBand()));
        s.setPronunciationBand(band(result.pronunciationBand()));
        s.setGradingResult(rawResult);
        s.setGradedAt(OffsetDateTime.now());
        s.setLastGradingError(null);
        sessions.save(s);
        log.info("Speaking session {} graded: overall {}", sessionId, result.overallBand());
    }

    /** Fail grading: {@code grading → grading_failed}; refund Lúa if deducted (idempotent). */
    @Transactional
    public void finishFailure(long sessionId, String error) {
        SpeakingSession s = sessions.findByIdForUpdate(sessionId).orElse(null);
        if (s == null || s.getStatus() != SpeakingSessionStatus.GRADING) {
            return;
        }
        s.setStatus(SpeakingSessionStatus.GRADING_FAILED);
        s.setGradedAt(OffsetDateTime.now());
        s.setLastGradingError(error == null ? "grading failed" : error.substring(0, Math.min(error.length(), 1000)));
        sessions.save(s);
        if (Boolean.TRUE.equals(s.getLuaDeducted()) && s.getLuaCost() != null && s.getLuaCost() > 0) {
            billing.refund(s.getUserId(), sessionId, s.getLuaCost());
        }
        log.warn("Speaking session {} grading failed: {}", sessionId, s.getLastGradingError());
    }

    private BigDecimal band(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
