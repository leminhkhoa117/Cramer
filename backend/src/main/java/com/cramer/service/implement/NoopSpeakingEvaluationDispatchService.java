package com.cramer.service.implement;

import com.cramer.service.SpeakingEvaluationDispatchService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoopSpeakingEvaluationDispatchService implements SpeakingEvaluationDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(NoopSpeakingEvaluationDispatchService.class);

    @Override
    public void dispatchEvaluation(Long sessionId, UUID userId) {
        logger.info("Speaking evaluation dispatch requested for session {} and user {}, but no grading worker is wired yet.",
                sessionId, userId);
    }
}
