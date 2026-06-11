package com.cramer.speaking.grading;

import com.cramer.speaking.service.SpeakingGradingTrigger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Implements {@link SpeakingGradingTrigger} (SPEC-14 §6): enqueues grading on the bounded
 * {@code speakingGradingExecutor} so a completed session is graded off the request thread. The
 * session service calls this after the completion transaction commits.
 */
@Component
public class SpeakingGradingDispatcher implements SpeakingGradingTrigger {

    private final SpeakingGradingWorker worker;

    public SpeakingGradingDispatcher(SpeakingGradingWorker worker) {
        this.worker = worker;
    }

    @Override
    @Async("speakingGradingExecutor")
    public void enqueue(long sessionId) {
        worker.grade(sessionId);
    }
}
