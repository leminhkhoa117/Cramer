package com.cramer.speaking.grading;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterChatRequest;
import com.cramer.platform.integration.openrouter.OpenRouterChatResult;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingTranscript;
import com.cramer.speaking.repository.SpeakingTranscriptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Speaking grading worker (SPEC-14 §6). Claims a completed session, builds a text-only prompt
 * from the persisted transcripts, calls OpenRouter (default {@code google/gemini-2.5-flash}),
 * validates the criterion bands, and persists the result — or fails + refunds after retries.
 *
 * <p>The slow OpenRouter call runs <em>outside</em> any DB transaction; transactional state
 * changes are delegated to {@link SpeakingGradingStore}. Audio multimodal grading is not wired
 * (the speaking realtime/audio deps were removed for a CVE); grading runs in {@code text_only}
 * mode from REST-persisted transcripts.
 */
@Component
public class SpeakingGradingWorker {

    private static final Logger log = LoggerFactory.getLogger(SpeakingGradingWorker.class);
    private static final int MAX_ATTEMPTS = 2;

    private final SpeakingGradingStore store;
    private final SpeakingTranscriptRepository transcripts;
    private final SpeakingGradingPromptBuilder prompts;
    private final OpenRouterClient openRouter;
    private final String model;

    public SpeakingGradingWorker(SpeakingGradingStore store, SpeakingTranscriptRepository transcripts,
                                 SpeakingGradingPromptBuilder prompts, OpenRouterClient openRouter,
                                 @Value("${speaking.grading.model:google/gemini-2.5-flash}") String model) {
        this.store = store;
        this.transcripts = transcripts;
        this.prompts = prompts;
        this.openRouter = openRouter;
        this.model = model;
    }

    /** Grade a session end-to-end. Safe to call for a non-claimable session (no-op). */
    public void grade(long sessionId) {
        if (!store.claim(sessionId)) {
            log.debug("Speaking session {} not claimable for grading", sessionId);
            return;
        }
        if (!openRouter.isConfigured()) {
            store.finishFailure(sessionId, "OpenRouter API key not configured");
            return;
        }
        SpeakingSession session = store.load(sessionId);
        if (session == null) {
            return;
        }
        List<SpeakingTranscript> turns = transcripts.findBySessionIdOrderByTurnIndexAsc(sessionId);
        String system = prompts.systemPrompt();
        String user = prompts.userPrompt(session, turns);

        String lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                OpenRouterChatRequest request = new OpenRouterChatRequest(
                        model, system, user, "speaking_grading", null, 0.3, 0, null, false, false);
                OpenRouterChatResult chat = openRouter.chat(request);
                JsonNode raw = chat.content();
                SpeakingGradingResult result = Json.mapper().treeToValue(raw, SpeakingGradingResult.class);
                if (result.bandsValid()) {
                    store.finishSuccess(sessionId, result, raw);
                    return;
                }
                lastError = "Model returned invalid/missing bands";
                log.warn("Speaking grading attempt {} for session {} produced invalid bands", attempt, sessionId);
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Speaking grading attempt {} for session {} failed: {}", attempt, sessionId, e.getMessage());
            }
        }
        store.finishFailure(sessionId, lastError);
    }
}
