package com.cramer.service.implement;

import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SpeakingSession;
import com.cramer.repository.SpeakingSessionRepository;
import com.cramer.service.CreditService;
import com.cramer.service.SpeakingEvaluationDispatchService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SpeakingGradingWatchdog {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingGradingWatchdog.class);

    @Value("${speaking.evaluation.watchdog.stuck-grading-minutes:10}")
    private int stuckGradingMinutes;

    @Value("${speaking.evaluation.watchdog.stuck-completed-minutes:5}")
    private int stuckCompletedMinutes;

    @Value("${speaking.evaluation.enabled:true}")
    private boolean evaluationEnabled;

    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingEvaluationDispatchService speakingEvaluationService;
    private final CreditService creditService;

    public SpeakingGradingWatchdog(
            SpeakingSessionRepository sessionRepository,
            SpeakingEvaluationDispatchService speakingEvaluationService,
            CreditService creditService) {
        this.sessionRepository = sessionRepository;
        this.speakingEvaluationService = speakingEvaluationService;
        this.creditService = creditService;
    }

    @Scheduled(fixedDelayString = "${speaking.evaluation.watchdog.interval-ms:60000}")
    public void sweep() {
        if (!evaluationEnabled) {
            logger.info("metric=speaking_grading_watchdog_skipped reason=kill_switch");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime gradingThreshold = now.minusMinutes(stuckGradingMinutes);
        OffsetDateTime completedThreshold = now.minusMinutes(stuckCompletedMinutes);

        List<SpeakingSession> stuckGrading = sessionRepository
                .findByStatusAndUpdatedAtBefore("grading", gradingThreshold);

        List<SpeakingSession> stuckCompleted = sessionRepository
                .findByStatusAndUpdatedAtBefore("completed", completedThreshold);

        for (SpeakingSession session : stuckGrading) {
            try {
                long staleMs = Duration.between(session.getUpdatedAt(), now).toMillis();
                long staleMinutes = staleMs / 60_000;

                logger.warn("metric=speaking_grading_watchdog_action sessionId={} action=fail-and-refund staleMinutes={}",
                        session.getId(), staleMinutes);

                SpeakingSessionStatusTransitioner.transitionTo(session.getStatus(), "grading_failed");
                session.setStatus("grading_failed");
                session.setGradedAt(now);
                sessionRepository.save(session);

                Integer luaCost = session.getLuaCost();
                if (luaCost != null && luaCost > 0 && Boolean.TRUE.equals(session.getLuaDeducted())) {
                    creditService.refundCredits(session.getUserId(), luaCost,
                            CreditTransaction.Category.SPEAKING_REFUND,
                            "Watchdog refund — grading timed out",
                            "refund_session_" + session.getId());
                    logger.info("metric=speaking_grading_refund sessionId={} amount={} trigger=watchdog",
                            session.getId(), luaCost);
                }
            } catch (Exception e) {
                logger.error("metric=speaking_grading_watchdog_error sessionId={} action=fail-and-refund error={}",
                        session.getId(), e.getMessage(), e);
            }
        }

        for (SpeakingSession session : stuckCompleted) {
            try {
                long staleMs = Duration.between(session.getUpdatedAt(), now).toMillis();
                long staleMinutes = staleMs / 60_000;

                logger.warn("metric=speaking_grading_watchdog_action sessionId={} action=re-enqueue staleMinutes={}",
                        session.getId(), staleMinutes);

                speakingEvaluationService.dispatchEvaluation(session.getId(), session.getUserId());
            } catch (Exception e) {
                logger.error("metric=speaking_grading_watchdog_error sessionId={} action=re-enqueue error={}",
                        session.getId(), e.getMessage(), e);
            }
        }

        logger.debug("metric=speaking_grading_watchdog_sweep stuckGrading={} stuckCompleted={}",
                stuckGrading.size(), stuckCompleted.size());
    }
}
