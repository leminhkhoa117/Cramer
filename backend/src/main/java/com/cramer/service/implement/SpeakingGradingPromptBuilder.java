package com.cramer.service.implement;

import com.cramer.dto.SpeakingTranscriptDTO;
import com.cramer.dto.SpeakingTurnDTO;
import com.cramer.service.abts.OpenRouterClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpeakingGradingPromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SpeakingGradingPromptBuilder.class);

    @Value("${speaking.evaluation.model:google/gemini-2.5-flash}")
    private String defaultModel;

    private final Map<Integer, String> partNames = Map.of(
            1, "PART 1",
            2, "PART 2",
            3, "PART 3"
    );

    @PostConstruct
    public void validateBandDescriptors() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("speaking/ielts_band_descriptors.md");
        if (is == null) {
            throw new IllegalStateException(
                    "ielts_band_descriptors.md not found in classpath — Speaking grading cannot run without rubric grounding");
        }
        try {
            is.close();
        } catch (IOException ignored) {
        }
        logger.info("speaking.ielts_band_descriptors validated — classpath resource available");
    }

    public String buildSystemPrompt(String sessionMode, List<Integer> partsIncluded) {
        String descriptors = loadBandDescriptors();

        StringBuilder sb = new StringBuilder();

        sb.append("You are an IELTS Speaking examiner. Grade the candidate's performance.\n\n");

        sb.append("## Session Metadata\n");
        sb.append("- Mode: ").append(sessionMode != null ? sessionMode : "full_test").append("\n");
        sb.append("- Parts active: ").append(partsIncluded != null ? partsIncluded.toString() : "[1, 2, 3]").append("\n");
        sb.append("- Accent: as specified by session\n");
        sb.append("- Speed: as specified by session\n\n");

        sb.append("## IELTS Speaking Band Descriptors (Public Version)\n");
        sb.append("Use these official descriptors as your rubric when assigning band scores.\n\n");
        sb.append(descriptors).append("\n\n");

        sb.append("## Operational Definitions\n");

        sb.append("### Hesitation\n");
        sb.append("- Definition: silent pause ≥2.0s OR filler word (um/uh/er/erm/ah/like/you know/sort of/kind of) used as a stalling device.\n");
        sb.append("- The `hesitations` list must contain perPart counts capped at 20 per part.\n");
        sb.append("- Each hesitation object must have: `location` (string), `timestamp` (seconds from question start).\n\n");

        sb.append("### Location Categories\n");
        sb.append("- `middle_of_speech`: within an ongoing utterance\n");
        sb.append("- `start_of_new_idea`: at a topic transition point\n");
        sb.append("- `end_of_idea`: after completing a point, before moving on\n");
        sb.append("- `between_clauses`: at a grammatical boundary\n");
        sb.append("- `filler_hesitation`: hesitation primarily composed of filler words\n");
        sb.append("- `other`: use sparingly, must not exceed 20% of total hesitations\n");
        sb.append("- Categories are non-overlapping. Self-corrections are NOT hesitations.\n\n");

        sb.append("### Pronunciation Confidence\n");
        sb.append("- `high`: clear audio, reliable assessment of stress/intonation/pronunciation\n");
        sb.append("- `medium`: adequate audio quality for assessment (default when audio is usable)\n");
        sb.append("- `low`: noisy, clipped, or short audio — set confidence to \"low\" and return empty arrays for inaccurateStresses, inaccurateIntonations, inaccuratePronunciations\n\n");

        sb.append("## Rubric Instructions\n");

        sb.append("### Grading Mode & Degraded Reason\n");
        sb.append("- `gradingMode`: \"full\" (default) or \"degraded\". Set \"degraded\" when audio quality severely limits assessment.\n");
        sb.append("- `degradedReason`: null if gradingMode is \"full\". Otherwise provide a short reason string.\n\n");

        sb.append("### All Criteria with Detailed Sub-fields\n");
        sb.append("For each of the 4 criteria (FluencyCoherence, LexicalResource, GrammaticalRangeAccuracy, Pronunciation), provide:\n");
        sb.append("- `band`: numeric band score (0.0 to 9.0, in 0.5 increments)\n");
        sb.append("- `justification`: 2-4 sentence explanation referencing the band descriptors, in Vietnamese\n");
        sb.append("- `strengths`: array of 1-3 specific strengths observed (in English)\n");
        sb.append("- `weaknesses`: array of 1-3 specific areas for improvement (in English)\n");
        sb.append("- `examples`: array of 1-3 concrete examples from the candidate's speech (in English, direct quotes from transcript)\n\n");

        sb.append("### Pronunciation-specific Sub-fields\n");
        sb.append("- `confidence`: \"high\" | \"medium\" | \"low\"\n");
        sb.append("- `inaccurateStresses`: array of words/phrases with incorrect word stress (empty if confidence is \"low\")\n");
        sb.append("- `inaccurateIntonations`: array of phrases where intonation pattern was inappropriate (empty if confidence is \"low\")\n");
        sb.append("- `inaccuratePronunciations`: array of words/phrases that were mispronounced (empty if confidence is \"low\")\n\n");

        sb.append("### perPartFeedback\n");
        sb.append("- For each part (1, 2, 3) that is active, provide a `perPartFeedback` entry:\n");
        sb.append("  - `partNumber`: integer\n");
        sb.append("  - `summary`: 1-2 sentence overall assessment for this part, in Vietnamese\n");
        sb.append("  - `bandEstimate`: approximate band for this part (0.0-9.0)\n\n");

        sb.append("### perTurnFeedback\n");
        sb.append("- For each turn, provide `perTurnFeedback`:\n");
        sb.append("  - `turnIndex`: integer matching the turn\n");
        sb.append("  - `comment`: 1 sentence observation about this specific response, in Vietnamese\n");
        sb.append("  - `relevantCriterion`: which criterion this comment primarily relates to (fluency/lexical/grammar/pronunciation)\n\n");

        sb.append("### improvementTips\n");
        sb.append("- Provide 3-5 actionable improvement tips in Vietnamese.\n");
        sb.append("- Each tip should be practical, specific, and linked to an observed weakness.\n\n");

        sb.append("### Sample Answers (Band 8 exemplar)\n");
        sb.append("- For each part, provide a `sampleAnswer`:\n");
        sb.append("  - Part 1/3: 50-120 words\n");
        sb.append("  - Part 2: 150-220 words\n");
        sb.append("  - Must be in natural spoken English at Band 8 level\n");
        sb.append("  - Reflects the same question/context as the candidate responded to\n\n");

        sb.append("### Language of Feedback\n");
        sb.append("- Free-text fields (justification, summary, comment, tips): respond in Vietnamese\n");
        sb.append("- Structured/quoted fields (strengths, weaknesses, examples, sampleAnswer): respond in English\n\n");

        sb.append("## Output Contract\n");
        sb.append("Return JSON matching schema `speaking_grading_v2` strictly. Do not include any text outside the JSON object.\n");

        return sb.toString();
    }

    public List<OpenRouterClient.ContentPart> buildUserContent(
            List<SpeakingTurnDTO> turns,
            List<SpeakingTranscriptDTO> transcripts,
            List<SpeakingAudioPreparer.PreparedAudio> preparedAudios,
            JsonNode sessionBlueprint,
            boolean textOnly) {

        List<OpenRouterClient.ContentPart> parts = new ArrayList<>();

        String mode = sessionBlueprint != null && sessionBlueprint.has("sessionMode")
                ? sessionBlueprint.get("sessionMode").asText() : "full_test";
        String accent = sessionBlueprint != null && sessionBlueprint.has("accent")
                ? sessionBlueprint.get("accent").asText() : "not_specified";
        String speed = sessionBlueprint != null && sessionBlueprint.has("speed")
                ? sessionBlueprint.get("speed").asText() : "1.0";

        List<Integer> activeParts = new ArrayList<>();
        if (sessionBlueprint != null && sessionBlueprint.has("partsIncluded")) {
            JsonNode partsNode = sessionBlueprint.get("partsIncluded");
            if (partsNode.isArray()) {
                for (JsonNode p : partsNode) {
                    activeParts.add(p.asInt());
                }
            }
        }
        if (activeParts.isEmpty()) {
            activeParts = List.of(1, 2, 3);
        }

        parts.add(new OpenRouterClient.TextPart(
                String.format("Session: mode=%s, accent=%s, speed=%s, parts=%s",
                        mode, accent, speed, activeParts.toString())));

        StringBuilder turnText = new StringBuilder();

        List<Integer> sortedPartNumbers = activeParts.stream().sorted().toList();
        for (Integer partNum : sortedPartNumbers) {
            turnText.append("=== ").append(partNames.getOrDefault(partNum, "PART " + partNum)).append(" ===\n\n");

            List<SpeakingTurnDTO> partTurns = turns.stream()
                    .filter(t -> t.getPartNumber() != null && t.getPartNumber().equals(partNum))
                    .sorted(Comparator.comparing(SpeakingTurnDTO::getTurnIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            for (SpeakingTurnDTO turn : partTurns) {
                int tn = turn.getTurnIndex() != null ? turn.getTurnIndex() : 0;
                String promptText = "";
                if (turn.getQuestionSnapshot() != null && turn.getQuestionSnapshot().has("promptText")) {
                    promptText = turn.getQuestionSnapshot().get("promptText").asText();
                }

                String transcriptText = findTranscriptText(transcripts, tn);

                turnText.append("--- Turn ").append(tn)
                        .append(" (Part ").append(partNum)
                        .append(") Q: ").append(promptText)
                        .append(" ---\n")
                        .append("Transcript: ").append(transcriptText).append("\n\n");
            }
        }

        parts.add(new OpenRouterClient.TextPart(turnText.toString()));

        if (!textOnly) {
            List<SpeakingAudioPreparer.PreparedAudio> sortedAudios = preparedAudios != null
                    ? preparedAudios.stream()
                            .sorted(Comparator.comparingInt(SpeakingAudioPreparer.PreparedAudio::turnIndex))
                            .toList()
                    : List.of();

            for (SpeakingAudioPreparer.PreparedAudio audio : sortedAudios) {
                String fmt = audio.format() != null ? audio.format() : "mp3";
                parts.add(new OpenRouterClient.AudioPart(
                        new OpenRouterClient.AudioInput(audio.base64Data(), fmt)));
            }
        }

        if (textOnly) {
            parts.add(new OpenRouterClient.TextPart(
                    "pronunciation.confidence MUST be 'low'. Set inaccurateStresses, inaccurateIntonations, " +
                            "inaccuratePronunciations to empty arrays []."));
        }

        parts.add(new OpenRouterClient.TextPart(
                "Return JSON matching schema speaking_grading_v2 strictly."));

        return parts;
    }

    private String loadBandDescriptors() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("speaking/ielts_band_descriptors.md");
            if (is == null) {
                throw new IllegalStateException("ielts_band_descriptors.md not found on classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ielts_band_descriptors.md: " + e.getMessage(), e);
        }
    }

    private String findTranscriptText(List<SpeakingTranscriptDTO> transcripts, int turnIndex) {
        if (transcripts == null) {
            return "(no transcript available)";
        }
        return transcripts.stream()
                .filter(t -> t.getTurnIndex() != null && t.getTurnIndex() == turnIndex)
                .findFirst()
                .map(SpeakingTranscriptDTO::getTranscriptText)
                .filter(text -> text != null && !text.isBlank())
                .orElse("(no transcript available)");
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
