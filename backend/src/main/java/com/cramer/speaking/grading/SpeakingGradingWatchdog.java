package com.cramer.speaking.grading;

import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingSessionStatus;
import com.cramer.speaking.repository.SpeakingSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Speaking grading watchdog (SPEC-14 §6): every 60 s, fail+refund {@code grading} sessions stale
 * &gt; 10 min and re-enqueue {@code completed} sessions stale &gt; 5 min (lost dispatches). Guarded
 * by {@code speaking.evaluation.enabled} (default true).
 */
@Component
public class SpeakingGradingWatchdog {

    private static final Logger log = LoggerFactory.getLogger(SpeakingGradingWatchdog.class);

    private final SpeakingSessionRepository sessions;
    private final SpeakingGradingStore store;
    private final SpeakingGradingWorker worker;
    private final boolean enabled;

    public SpeakingGradingWatchdog(SpeakingSessionRepository sessions, SpeakingGradingStore store,
                                   SpeakingGradingWorker worker,
                                   @Value("${speaking.evaluation.enabled:true}") boolean enabled) {
        this.sessions = sessions;
        this.store = store;
        this.worker = worker;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${speaking.evaluation.watchdog-ms:60000}")
    public void sweep() {
        if (!enabled) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<SpeakingSession> stuckGrading = sessions.findStuck(SpeakingSessionStatus.GRADING, now.minusMinutes(10));
        for (SpeakingSession s : stuckGrading) {
            log.warn("Watchdog: failing stale grading session {}", s.getId());
            store.finishFailure(s.getId(), "Grading timed out (watchdog)");
        }
        List<SpeakingSession> stuckCompleted = sessions.findStuck(SpeakingSessionStatus.COMPLETED, now.minusMinutes(5));
        for (SpeakingSession s : stuckCompleted) {
            log.info("Watchdog: re-enqueuing un-graded completed session {}", s.getId());
            worker.grade(s.getId());
        }
    }
}
