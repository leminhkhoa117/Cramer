package com.cramer.abts.generation;

import com.cramer.abts.config.AbtsProperties;
import com.cramer.abts.domain.GenerationResult;
import com.cramer.abts.domain.GenerationStatus;
import com.cramer.abts.domain.StreamEvent;
import com.cramer.abts.validation.ContentValidator;
import com.cramer.abts.validation.ValidationResult;
import com.cramer.abts.web.dto.GenerationRequest;
import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterException;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

/**
 * Orchestrates ABTS generation (SPEC-21): resolves the per-skill generator, runs each requested
 * part through a ≤3-attempt validate-retry loop (phases cached across attempts), renumbers + merges
 * multi-part results, and aggregates partial success/usage. Used by both the synchronous and
 * streaming paths (the latter passes an emitter + cancellation flag).
 *
 * <p>Resilience: retries wait on an exponential backoff with jitter and honor the upstream
 * Retry-After hint; each part has a deadline (SPEC-21 §6, {@code abts.streaming.part-timeout-ms})
 * after which it fails with {@code PART_TIMEOUT}.</p>
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long MAX_BACKOFF_MS = 10_000;
    private static final long MAX_RETRY_AFTER_MS = 30_000;

    private final Map<Skill, PartGenerator> generators = new EnumMap<>(Skill.class);
    private final ContentValidator validator;
    private final QuestionRenumberer renumberer;
    private final ModelResolver modelResolver;
    private final OpenRouterClient client;
    private final OpenRouterProperties props;
    private final AbtsProperties abtsProps;

    public GenerationService(List<PartGenerator> generatorBeans, ContentValidator validator,
                             QuestionRenumberer renumberer, ModelResolver modelResolver,
                             OpenRouterClient client, OpenRouterProperties props,
                             AbtsProperties abtsProps) {
        for (PartGenerator g : generatorBeans) {
            generators.put(g.skill(), g);
        }
        this.validator = validator;
        this.renumberer = renumberer;
        this.modelResolver = modelResolver;
        this.client = client;
        this.props = props;
        this.abtsProps = abtsProps;
    }

    /** Synchronous generation (no streaming). */
    public GenerationResult generate(Skill skill, GenerationRequest request) {
        return generate(skill, request, StreamEmitter.NOOP, () -> false, false);
    }

    /**
     * Generate content for the requested parts. Cooperative cancellation is checked between parts;
     * {@link GenerationCancelledException} propagates to the streaming caller.
     */
    public GenerationResult generate(Skill skill, GenerationRequest request, StreamEmitter emitter,
                                     BooleanSupplier cancelled, boolean streaming) {
        if (skill == Skill.SPEAKING) {
            return GenerationResult.notImplemented("speaking");
        }
        PartGenerator generator = generators.get(skill);
        if (generator == null) {
            return GenerationResult.notImplemented(skill.name().toLowerCase());
        }
        List<Integer> parts = request.safeParts();
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("At least one part/task is required");
        }
        String model = modelResolver.resolve(request.safeModel().model());
        JsonNode reasoning = modelResolver.reasoningPayload(model, request.safeModel());
        GenerationContext ctx = new GenerationContext(client, model, request.safeModel(), reasoning,
                streaming, emitter, cancelled, request.resolvedLanguage(), request.customInstructions());

        List<PartOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            ctx.checkCancelled();
            int part = parts.get(i);
            emitter.emit(StreamEvent.progress(pct(i, parts.size()), part, parts.size(),
                    "Starting part " + part));
            outcomes.add(generatePart(skill, generator, part, request, ctx));
        }
        return aggregate(skill, model, parts.size(), outcomes, ctx);
    }

    /**
     * Regenerate questions against an existing passage/transcript (SPEC-21 §9). Reading/Listening
     * require {@code existingPassageText} (400 if missing); the upstream passage/transcript phase is
     * seeded so only questions/answers are regenerated. Writing delegates to full generation;
     * Speaking is {@code NOT_IMPLEMENTED}.
     */
    public GenerationResult regenerateQuestions(Skill skill, GenerationRequest request) {
        if (skill == Skill.SPEAKING) {
            return GenerationResult.notImplemented("speaking");
        }
        if (skill == Skill.WRITING) {
            return generate(skill, request);
        }
        String existing = request.existingPassageText();
        if (existing == null || existing.isBlank()) {
            throw new IllegalArgumentException("existingPassageText is required to regenerate questions");
        }
        PartGenerator generator = generators.get(skill);
        List<Integer> parts = request.safeParts();
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("At least one part is required");
        }
        int part = parts.get(0);
        String model = modelResolver.resolve(request.safeModel().model());
        JsonNode reasoning = modelResolver.reasoningPayload(model, request.safeModel());
        GenerationContext ctx = new GenerationContext(client, model, request.safeModel(), reasoning,
                false, StreamEmitter.NOOP, () -> false, request.resolvedLanguage(), request.customInstructions());

        if (skill == Skill.READING) {
            ObjectNode passage = Json.mapper().createObjectNode();
            passage.putObject("section").put("passage_text", existing);
            ctx.seedPhase(part, "passage", passage);
        } else {
            ObjectNode transcript = Json.mapper().createObjectNode();
            transcript.put("transcript", existing);
            transcript.put("audio_placeholder", "");
            ctx.seedPhase(part, "transcript", transcript);
        }
        PartOutcome outcome = generatePart(skill, generator, part, request, ctx);
        return aggregate(skill, model, 1, List.of(outcome), ctx);
    }

    // ---------------------------------------------------------------- per-part

    private PartOutcome generatePart(Skill skill, PartGenerator generator, int part,
                                     GenerationRequest request, GenerationContext ctx) {
        PartConfig cfg = request.partConfig(part);
        String taskType = cfg == null ? null : cfg.taskType();
        ValidationResult lastValidation = null;
        JsonNode lastContent = null;
        long deadlineNanos = System.nanoTime() + partTimeoutMs() * 1_000_000L;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (System.nanoTime() > deadlineNanos) {
                log.warn("Part {} exceeded the per-part timeout ({} ms)", part, partTimeoutMs());
                return PartOutcome.failure(part, "PART_TIMEOUT", true, attempt);
            }
            try {
                JsonNode content = generator.generatePart(part, cfg, ctx);
                ValidationResult validation = validator.validate(skill, part, taskType, content);
                lastContent = content;
                lastValidation = validation;
                if (validation.isValid()) {
                    return PartOutcome.success(part, content, validation, attempt);
                }
                if (attempt < MAX_ATTEMPTS) {
                    ctx.emit(StreamEvent.retry(attempt, MAX_ATTEMPTS,
                            "Validation found issues; retrying part " + part));
                }
            } catch (OpenRouterException e) {
                log.warn("OpenRouter error on part {} attempt {}: {} ({})", part, attempt, e.getMessage(), e.error());
                if (e.retryable() && attempt < MAX_ATTEMPTS) {
                    long delay = retryDelayMs(attempt, e.retryAfterMs());
                    ctx.emit(StreamEvent.retry(attempt, MAX_ATTEMPTS,
                            "Upstream error; retrying part " + part + " in " + delay + " ms"));
                    sleep(delay);
                    continue;
                }
                return PartOutcome.failure(part, e.error().name(), e.retryable(), attempt);
            }
        }
        // Parseable but still has issues after retries → keep content (PARTIAL_SUCCESS upstream).
        if (lastContent != null) {
            return PartOutcome.success(part, lastContent, lastValidation, MAX_ATTEMPTS);
        }
        return PartOutcome.failure(part, "UPSTREAM_ERROR", true, MAX_ATTEMPTS);
    }

    // ---------------------------------------------------------------- retry policy

    private static long retryDelayMs(int attempt, Long retryAfterMs) {
        long backoff = Math.min(1_000L << (attempt - 1), MAX_BACKOFF_MS);
        long withJitter = backoff + ThreadLocalRandom.current().nextLong(0, 500);
        return retryAfterMs != null
                ? Math.max(withJitter, Math.min(retryAfterMs, MAX_RETRY_AFTER_MS))
                : withJitter;
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenerationCancelledException("Interrupted while waiting to retry");
        }
    }

    private int partTimeoutMs() {
        int configured = abtsProps.streaming().partTimeoutMs();
        return configured <= 0 ? 600_000 : configured;
    }

    // ---------------------------------------------------------------- aggregate

    private GenerationResult aggregate(Skill skill, String model, int totalParts,
                                       List<PartOutcome> outcomes, GenerationContext ctx) {
        List<PartOutcome> successes = outcomes.stream().filter(o -> !o.failed()).toList();
        Map<Integer, String> partErrors = new LinkedHashMap<>();
        outcomes.stream().filter(PartOutcome::failed)
                .forEach(o -> partErrors.put(o.part(), o.errorCode()));

        int attempts = outcomes.stream().mapToInt(PartOutcome::attempts).max().orElse(0);

        if (successes.isEmpty()) {
            String code = outcomes.isEmpty() ? "UPSTREAM_ERROR" : outcomes.get(0).errorCode();
            boolean retryable = !outcomes.isEmpty() && outcomes.get(0).retryable();
            GenerationResult failed = GenerationResult.failed(skill.name().toLowerCase(), model, attempts, code, retryable);
            return new GenerationResult(failed.status(), failed.skill(), null, null, partErrors,
                    ctx.reasoningText(), ctx.usage(), model, attempts, code, retryable);
        }

        boolean multiPart = totalParts > 1;
        JsonNode content = mergeContent(skill, successes, multiPart);
        ValidationResult combined = new ValidationResult();
        successes.forEach(o -> combined.addAll(o.validation()));

        boolean anyErrors = !partErrors.isEmpty() || !combined.isValid();
        GenerationStatus status = anyErrors ? GenerationStatus.PARTIAL_SUCCESS : GenerationStatus.SUCCESS;

        return new GenerationResult(status, skill.name().toLowerCase(), content, combined.toView(),
                partErrors.isEmpty() ? null : partErrors, ctx.reasoningText(), ctx.usage(), model, attempts, null, null);
    }

    private JsonNode mergeContent(Skill skill, List<PartOutcome> successes, boolean multiPart) {
        if (!multiPart) {
            return successes.get(0).content();
        }
        ArrayNode sections = Json.mapper().createArrayNode();
        for (PartOutcome outcome : successes) {
            JsonNode part = outcome.content();
            if (skill == Skill.READING || skill == Skill.LISTENING) {
                renumberer.renumber(part, skill, outcome.part());
            }
            ObjectNode section = part.deepCopy();
            section.put("part", outcome.part());
            sections.add(section);
        }
        ObjectNode root = Json.mapper().createObjectNode();
        root.set("sections", sections);
        return root;
    }

    private int pct(int index, int total) {
        return total == 0 ? 0 : (int) Math.round((index * 100.0) / total);
    }
}
