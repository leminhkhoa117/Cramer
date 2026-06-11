package com.cramer.abts.generation;

import com.cramer.abts.config.AbtsProperties;
import com.cramer.abts.domain.Hunk;
import com.cramer.abts.validation.ContentValidator;
import com.cramer.abts.validation.ValidationResult;
import com.cramer.abts.web.dto.RefinementApplyRequest;
import com.cramer.abts.web.dto.RefinementApplyResponse;
import com.cramer.abts.web.dto.RefinementRequest;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterChatRequest;
import com.cramer.platform.integration.openrouter.OpenRouterChatResult;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterStreamListener;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.abts.domain.StreamEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Single, coherent refinement flow (SPEC-23 §5): the model proposes targeted patches for the
 * selected issues; patches are applied to a copy of the content and surfaced as diff
 * {@link Hunk}s for author accept/reject; accepted hunks are applied on {@code /refine/apply}.
 * Empty selection is rejected (400); the round counter is capped by {@code abts.max-refinement-rounds}.
 */
@Service
public class RefinementService {

    private static final Logger log = LoggerFactory.getLogger(RefinementService.class);

    private final OpenRouterClient client;
    private final ModelResolver modelResolver;
    private final ContentValidator validator;
    private final int maxRounds;

    public RefinementService(OpenRouterClient client, ModelResolver modelResolver,
                             ContentValidator validator, AbtsProperties props) {
        this.client = client;
        this.modelResolver = modelResolver;
        this.validator = validator;
        this.maxRounds = props.maxRefinementRounds();
    }

    /** Propose + apply patches, returning reviewable hunks. Streams content deltas via {@code emitter}. */
    public List<Hunk> refine(RefinementRequest request, StreamEmitter emitter, BooleanSupplier cancelled) {
        if (request.originalJson() == null || request.originalJson().isMissingNode()) {
            throw new IllegalArgumentException("originalJson is required");
        }
        if (request.safeIssueIds().isEmpty()) {
            throw new IllegalArgumentException("Select at least one issue to refine");
        }
        if (request.safeRound() >= maxRounds) {
            throw new OperationNotAllowedException("Maximum refinement rounds (" + maxRounds + ") reached");
        }

        String model = modelResolver.resolve(request.safeModel().model());
        JsonNode reasoning = modelResolver.reasoningPayload(model, request.safeModel());
        String system = "You are an IELTS content editor. Fix ONLY the selected issues with minimal, "
                + "targeted edits. Return strictly JSON: { \"patches\": [ { \"op\": \"replace|insert|append\", "
                + "\"questionNumber\": <n|null>, \"path\": \"/json/pointer\", \"value\": <new value> } ] }.";
        String user = String.join("\n\n",
                "Selected issue ids to fix: " + String.join(", ", request.safeIssueIds()),
                "Current validation:\n" + (request.validation() == null ? "{}" : Json.toJson(request.validation())),
                "Content to refine:\n" + Json.toJson(request.originalJson()),
                "Each patch targets either a question (set questionNumber + a path relative to the question, "
                        + "e.g. \"/correct_answer\") or an absolute document path (questionNumber null).");

        OpenRouterChatRequest chatRequest = new OpenRouterChatRequest(model, system, user, "refinement_patches",
                null, request.safeModel().resolvedTemperature(), request.safeModel().resolvedMaxTokens(),
                reasoning, false, request.safeModel().cacheEnabled());

        OpenRouterChatResult result;
        OpenRouterStreamListener listener = new OpenRouterStreamListener() {
            @Override
            public void onContentDelta(String delta) {
                emitter.emit(StreamEvent.aiChunk(delta, request.part()));
            }

            @Override
            public void onReasoningDelta(String delta) {
                emitter.emit(StreamEvent.aiThinking(delta, request.part()));
            }
        };
        result = client.streamChat(chatRequest, listener, cancelled);

        JsonNode patches = result.content().path("patches");
        return buildHunks(request.originalJson(), patches);
    }

    /** Apply only the accepted hunks; per-hunk failures are skipped (SPEC-23 §5.2). */
    public RefinementApplyResponse applyAccepted(RefinementApplyRequest request) {
        if (request.originalJson() == null || request.originalJson().isMissingNode()) {
            throw new IllegalArgumentException("originalJson is required");
        }
        JsonNode patched = request.originalJson().deepCopy();
        List<String> skipped = new ArrayList<>();
        for (Hunk hunk : request.safeHunks()) {
            try {
                applyHunk(patched, hunk);
            } catch (RuntimeException e) {
                log.debug("Skipping hunk {}: {}", hunk.id(), e.getMessage());
                skipped.add(hunk.id());
            }
        }
        ValidationResult validation = revalidate(request, patched);
        return new RefinementApplyResponse(patched, skipped, validation.toView());
    }

    // ---------------------------------------------------------------- internals

    private List<Hunk> buildHunks(JsonNode originalJson, JsonNode patches) {
        List<Hunk> hunks = new ArrayList<>();
        if (!patches.isArray()) {
            return hunks;
        }
        JsonNode working = originalJson.deepCopy();
        int i = 0;
        for (JsonNode patch : patches) {
            String op = patch.path("op").asText("replace").toLowerCase();
            String normalizedOp = op.equals("insert") || op.equals("append") ? "add" : op;
            String pointer = resolvePointer(working, patch);
            if (pointer == null) {
                continue;
            }
            JsonNode value = patch.path("value");
            try {
                JsonNode before = JsonPointerUtil.get(working, pointer);
                JsonNode beforeCopy = (before == null || before.isMissingNode()) ? null : before.deepCopy();
                applyOp(working, normalizedOp, pointer, value);
                hunks.add(new Hunk("hunk-" + (i++), normalizedOp, pointer, beforeCopy,
                        value.isMissingNode() ? null : value, describe(normalizedOp, pointer)));
            } catch (RuntimeException e) {
                log.debug("Patch {} not applicable: {}", pointer, e.getMessage());
            }
        }
        return hunks;
    }

    private void applyHunk(JsonNode root, Hunk hunk) {
        switch (hunk.op() == null ? "replace" : hunk.op().toLowerCase()) {
            case "remove" -> JsonPointerUtil.remove(root, hunk.path());
            default -> JsonPointerUtil.set(root, hunk.path(), hunk.after() == null
                    ? Json.mapper().nullNode() : hunk.after());
        }
    }

    private void applyOp(JsonNode root, String op, String pointer, JsonNode value) {
        if ("remove".equals(op)) {
            JsonPointerUtil.remove(root, pointer);
        } else {
            JsonPointerUtil.set(root, pointer, value.isMissingNode() ? Json.mapper().nullNode() : value);
        }
    }

    /** Resolve a patch's question-relative or absolute path to a document JSON pointer. */
    private String resolvePointer(JsonNode root, JsonNode patch) {
        String path = patch.path("path").asText("");
        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }
        if (patch.path("questionNumber").isInt()) {
            int qnum = patch.get("questionNumber").asInt();
            int idx = indexOfQuestion(root, qnum);
            if (idx < 0) {
                return null;
            }
            return "/questions/" + idx + path;
        }
        return path.isEmpty() ? null : path;
    }

    private int indexOfQuestion(JsonNode root, int questionNumber) {
        JsonNode questions = root.path("questions");
        if (!questions.isArray()) {
            return -1;
        }
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).path("question_number").asInt(-1) == questionNumber) {
                return i;
            }
        }
        return -1;
    }

    private ValidationResult revalidate(RefinementApplyRequest request, JsonNode patched) {
        try {
            Skill skill = Skill.valueOf(request.skill() == null ? "READING" : request.skill().trim().toUpperCase());
            int part = request.part() == null ? 1 : request.part();
            return validator.validate(skill, part, request.taskType(), patched);
        } catch (RuntimeException e) {
            return new ValidationResult();
        }
    }

    private String describe(String op, String pointer) {
        return op + " at " + pointer;
    }
}
