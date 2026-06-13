package com.cramer.speaking.grading;

import com.cramer.speaking.domain.SpeakingSession;
import com.cramer.speaking.domain.SpeakingTranscript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the Speaking grading prompt (SPEC-14 §6) from the frozen blueprint + persisted
 * transcripts. Text-only mode (no audio): the model is told pronunciation confidence is low and
 * to keep pronunciation detail conservative.
 */
@Component
public class SpeakingGradingPromptBuilder {

    public String systemPrompt() {
        return """
                You are a certified IELTS Speaking examiner. Grade the candidate strictly against the
                four official criteria: Fluency & Coherence, Lexical Resource, Grammatical Range &
                Accuracy, and Pronunciation. Bands are 0–9 in 0.5 steps. Return ONLY JSON:
                {
                  "schemaVersion": 1,
                  "overallBand": <number>,
                  "fluencyBand": <number>, "lexicalBand": <number>,
                  "grammarBand": <number>, "pronunciationBand": <number>,
                  "gradingMode": "text_only",
                  "degradedReason": "audio not available; pronunciation graded conservatively from transcript",
                  "criteria": { "fluency": {...}, "lexical": {...}, "grammar": {...}, "pronunciation": {...} },
                  "perPartFeedback": [ { "partNumber": <n>, "feedback": "..." } ],
                  "perTurnFeedback": [ { "turnIndex": <n>, "partNumber": <n>, "shortNote": "...", "sampleAnswer": "..." } ],
                  "improvementTips": [ "..." ]
                }
                The overall band must be the rounded average of the four criterion bands (0.5 steps).
                """;
    }

    public String userPrompt(SpeakingSession session, List<SpeakingTranscript> transcripts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Session mode: ").append(session.getSessionMode()).append('\n');
        sb.append("Accent: ").append(session.getAccent()).append("; speed: ").append(session.getSpeed()).append("\n\n");
        sb.append("Candidate responses (text-only transcripts):\n");
        if (transcripts.isEmpty()) {
            sb.append("(no transcripts captured)\n");
        }
        for (SpeakingTranscript t : transcripts) {
            String question = t.getQuestionSnapshot() == null ? "" : t.getQuestionSnapshot().path("text").asText("");
            if (question.isBlank() && t.getQuestionSnapshot() != null) {
                question = t.getQuestionSnapshot().toString();
            }
            sb.append("\n— Part ").append(t.getPartNumber()).append(", turn ").append(t.getTurnIndex()).append('\n');
            sb.append("  Examiner: ").append(question).append('\n');
            sb.append("  Candidate: ")
              .append(t.getTranscriptText() == null || t.getTranscriptText().isBlank()
                      ? "(no response)" : t.getTranscriptText())
              .append('\n');
        }
        sb.append("\nGrade now and return only the JSON object.");
        return sb.toString();
    }
}
