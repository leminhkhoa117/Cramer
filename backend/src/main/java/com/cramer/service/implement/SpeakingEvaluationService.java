package com.cramer.service.implement;

import com.cramer.service.SpeakingEvaluationDispatchService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SpeakingEvaluationService implements SpeakingEvaluationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingEvaluationService.class);

    private final SpeakingGradingWorker speakingGradingWorker;

    public SpeakingEvaluationService(SpeakingGradingWorker speakingGradingWorker) {
        this.speakingGradingWorker = speakingGradingWorker;
    }

    @Override
    @Async("speakingGradingExecutor")
    public void dispatchEvaluation(Long sessionId, UUID userId) {
        logger.info("metric=speaking_grading_dispatched sessionId={} userId={}", sessionId, userId);
        try {
            speakingGradingWorker.gradeSession(sessionId, userId);
        } catch (Exception e) {
            logger.error("metric=speaking_grading_dispatcher_error sessionId={} error={}",
                    sessionId, e.getMessage(), e);
        }
    }
}
