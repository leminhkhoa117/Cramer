package com.cramer.service;

import java.util.UUID;

public interface SpeakingEvaluationDispatchService {

    void dispatchEvaluation(Long sessionId, UUID userId);
}
