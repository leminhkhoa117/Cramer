package com.cramer.abts.generation;

import com.cramer.abts.config.AbtsProperties;
import com.cramer.abts.domain.Hunk;
import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.generation.prompt.PromptSchemaBuilder;
import com.cramer.abts.generation.prompt.RefinementPromptBuilder;
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
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Single, coherent refinement flow (SPEC-23 §5): the model proposes targeted patches for the
 * selected issues; patches are applied to a copy of the content and surfaced as diff
 * {@link Hunk}s for author accept/reject; accepted hunks are applied on {@code /refine/apply}.
 * Empty selection is rejected (400); the round counter is tracked server-side per content and
 * capped by {@code abts.max-refinement-rounds}.
 */
@Service
public class RefinementService {

    private static final Logger log = LoggerFactory.getLogger(RefinementService.class);
    private static final int MAX_TRACKED_CONTENTS = 500;

    private final OpenRouterClient client;
    private final ModelResolver modelResolver;
    private final ContentValidator validator;
    private final RefinementPromptBuilder prompts;
    private final PromptSchemaBuilder schemas;
    private final int maxRounds;

    /** Content-hash -> refine rounds so the cap cannot be bypassed by sending round: 0. */
    private final Map<Integer, AtomicInteger> roundsByContent = new ConcurrentHashMap<>();

    public RefinementService(OpenRouterClient client, ModelResolver modelResolver,
                             ContentValidator validator, RefinementPromptBuilder prompts,
                             PromptSchemaBuilder schemas, AbtsProperties props) {
        this.client = client;
        this.modelResolver = modelResolver;
        this.validator = validator;
        this.prompts = prompts;
        this.schemas = schemas;
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

        int effectiveRound = resolveRound(request);
        if (effectiveRound >= maxRounds) {
            throw new OperationNotAllowedException("Maximum refinement rounds (" + maxRounds + ") reached");
        }

        String model = modelResolver.resolve(request.safeModel().model());
        JsonNode reasoning = modelResolver.reasoningPayload(model, request.safeModel());

        OpenRouterChatRequest chatRequest = new OpenRouterChatRequest(model,
                prompts.systemPrompt(),
                prompts.userPrompt(request.safeIssueIds(), request.validation(), request.originalJson()),
                "refinement_patches", schemas.refinementPatchesSchema(),
                request.safeModel().resolvedTemperature(), request.safeModel().resolvedMaxTokens(),
                reasoning);

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
        OpenRouterChatResult result = client.streamChat(chatRequest, listener, cancelled);

        List<Hunk> hunks = buildHunks(request.originalJson(), result.content().path("patches"));
        if (!hunks.isEmpty()) {
            incrementRound(request.originalJson());
        }
        return hunks;
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

    // ---------------------------------------------------------------- round tracking

    private int resolveRound(RefinementRequest request) {
        AtomicInteger tracked = roundsByContent.get(hashOf(request.originalJson()));
        int trackedRound = tracked == null ? 0 : tracked.get();
        return Math.max(request.safeRound(), trackedRound);
    }

    private void incrementRound(JsonNode content) {
        if (roundsByContent.size() >= MAX_TRACKED_CONTENTS) {
            roundsByContent.clear();
        }
        roundsByContent.computeIfAbsent(hashOf(content), h -> new AtomicInteger()).incrementAndGet();
    }

    private static int hashOf(JsonNode content) {
        return Json.toJson(content).hashCode();
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
            ValidationResult failed = new ValidationResult();
            failed.addError("rv-revalidate-failed", "/", "Revalidation failed: " + e.getMessage());
            return failed;
        }
    }

    private String describe(String op, String pointer) {
        return op + " at " + pointer;
    }
}
