package com.cramer.speaking.grading;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterChatRequest;
import com.cramer.platform.integration.openrouter.OpenRouterChatResult;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.repository.SpeakingTranscriptRepository;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingGradingWorkerTest {

    @Mock SpeakingGradingStore store;
    @Mock SpeakingTranscriptRepository transcripts;
    @Mock OpenRouterClient openRouter;

    private SpeakingGradingWorker worker() {
        return new SpeakingGradingWorker(store, transcripts, new SpeakingGradingPromptBuilder(),
                openRouter, "google/gemini-2.5-flash");
    }

    private OpenRouterChatResult chatResult(double overall) {
        ObjectNode node = Json.mapper().createObjectNode();
        node.put("schemaVersion", 1);
        node.put("overallBand", overall);
        node.put("fluencyBand", overall);
        node.put("lexicalBand", overall);
        node.put("grammarBand", overall);
        node.put("pronunciationBand", overall);
        node.put("gradingMode", "text_only");
        return new OpenRouterChatResult(node, node.toString(), null, 10, 20, 30, 0.0, "google/gemini-2.5-flash");
    }

    @Test
    void unclaimableSessionIsNoOp() {
        when(store.claim(5L)).thenReturn(false);

        worker().grade(5L);

        verify(openRouter, never()).chat(any());
        verify(store, never()).finishSuccess(eq(5L), any(), any());
    }

    @Test
    void validGradingPersistsSuccess() {
        when(store.claim(5L)).thenReturn(true);
        when(openRouter.isConfigured()).thenReturn(true);
        SpeakingSession s = new SpeakingSession();
        s.setId(5L);
        s.setSessionMode("FULL");
        s.setAccent("british");
        when(store.load(5L)).thenReturn(s);
        when(transcripts.findBySessionIdOrderByTurnIndexAsc(5L)).thenReturn(List.of());
        when(openRouter.chat(any(OpenRouterChatRequest.class))).thenReturn(chatResult(7.0));

        worker().grade(5L);

        verify(store).finishSuccess(eq(5L), any(SpeakingGradingResult.class), any());
        verify(store, never()).finishFailure(eq(5L), any());
    }

    @Test
    void invalidBandsFailAfterRetries() {
        when(store.claim(5L)).thenReturn(true);
        when(openRouter.isConfigured()).thenReturn(true);
        SpeakingSession s = new SpeakingSession();
        s.setId(5L);
        when(store.load(5L)).thenReturn(s);
        when(transcripts.findBySessionIdOrderByTurnIndexAsc(5L)).thenReturn(List.of());
        when(openRouter.chat(any(OpenRouterChatRequest.class))).thenReturn(chatResult(11.0)); // out of range

        worker().grade(5L);

        verify(store).finishFailure(eq(5L), any());
        verify(store, never()).finishSuccess(eq(5L), any(), any());
    }
}
