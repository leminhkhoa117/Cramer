package com.cramer.abts.web;

import com.cramer.abts.config.AbtsProperties;
import com.cramer.abts.domain.GenerationResult;
import com.cramer.abts.generation.GenerationService;
import com.cramer.abts.generation.RefinementService;
import com.cramer.abts.generation.StreamingGenerationService;
import com.cramer.abts.service.AbtsSaveService;
import com.cramer.abts.service.ModelCatalogService;
import com.cramer.abts.service.TemplateService;
import com.cramer.abts.validation.ContentValidator;
import com.cramer.abts.validation.ValidationView;
import com.cramer.abts.web.dto.GenerationRequest;
import com.cramer.abts.web.dto.RefinementApplyRequest;
import com.cramer.abts.web.dto.RefinementApplyResponse;
import com.cramer.abts.web.dto.RefinementRequest;
import com.cramer.abts.web.dto.SaveContentRequest;
import com.cramer.abts.web.dto.SaveContentResponse;
import com.cramer.abts.web.dto.StatusResponse;
import com.cramer.platform.common.ielts.Skill;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * ABTS admin API (SPEC-25 §1). All routes are under {@code /api/admin/abts} and admin-gated by the
 * security chain (SPEC-04 §1). Errors use the global model — never a {@code 200 {success:false}}.
 */
@RestController
@RequestMapping("/api/admin/abts")
public class AbtsController {

    private final GenerationService generationService;
    private final StreamingGenerationService streamingService;
    private final RefinementService refinementService;
    private final ContentValidator validator;
    private final AbtsSaveService saveService;
    private final ModelCatalogService modelCatalog;
    private final TemplateService templateService;
    private final OpenRouterClient openRouter;
    private final OpenRouterProperties openRouterProps;
    private final AbtsProperties abtsProps;

    public AbtsController(GenerationService generationService, StreamingGenerationService streamingService,
                          RefinementService refinementService, ContentValidator validator,
                          AbtsSaveService saveService, ModelCatalogService modelCatalog,
                          TemplateService templateService, OpenRouterClient openRouter,
                          OpenRouterProperties openRouterProps, AbtsProperties abtsProps) {
        this.generationService = generationService;
        this.streamingService = streamingService;
        this.refinementService = refinementService;
        this.validator = validator;
        this.saveService = saveService;
        this.modelCatalog = modelCatalog;
        this.templateService = templateService;
        this.openRouter = openRouter;
        this.openRouterProps = openRouterProps;
        this.abtsProps = abtsProps;
    }

    // ---- generate ----

    @PostMapping("/generate/{skill}")
    public GenerationResult generate(@PathVariable String skill, @RequestBody GenerationRequest request) {
        return generationService.generate(parseSkill(skill), request);
    }

    @PostMapping(value = "/generate/{skill}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@PathVariable String skill, @RequestBody GenerationRequest request) {
        return streamingService.stream(parseSkill(skill), request);
    }

    @PostMapping("/generate/questions")
    public GenerationResult generateQuestions(@RequestParam String skill, @RequestBody GenerationRequest request) {
        return generationService.regenerateQuestions(parseSkill(skill), request);
    }

    // ---- validate ----

    @PostMapping("/validate")
    public ValidationView validate(@RequestParam String skill,
                                   @RequestParam(defaultValue = "1") int part,
                                   @RequestParam(required = false) String taskType,
                                   @RequestBody JsonNode content) {
        return validator.validate(parseSkill(skill), part, taskType, content).toView();
    }

    // ---- refine ----

    @PostMapping(value = "/refine/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter refineStream(@RequestBody RefinementRequest request) {
        return streamingService.streamRefinement(request);
    }

    @PostMapping("/refine/apply")
    public RefinementApplyResponse refineApply(@RequestBody RefinementApplyRequest request) {
        return refinementService.applyAccepted(request);
    }

    // ---- save ----

    @PostMapping("/save")
    public SaveContentResponse save(@RequestBody SaveContentRequest request) {
        return saveService.save(request);
    }

    // ---- catalog / status ----

    @GetMapping("/models")
    public ArrayNode models() {
        return modelCatalog.listModels();
    }

    @GetMapping("/models/capabilities/{id}")
    public JsonNode capability(@PathVariable String id) {
        return modelCatalog.capability(id);
    }

    @GetMapping("/templates")
    public ArrayNode templates() {
        return templateService.categories();
    }

    @GetMapping("/templates/{categoryId}")
    public ArrayNode templates(@PathVariable String categoryId) {
        return templateService.templates(categoryId);
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return new StatusResponse(
                openRouter.isConfigured(),
                openRouterProps.resolvedDefaultModel(),
                abtsProps.streaming().emitterTimeoutMs(),
                abtsProps.streaming().partTimeoutMs(),
                abtsProps.maxRefinementRounds(),
                "v1");
    }

    // ---- helpers ----

    private Skill parseSkill(String raw) {
        try {
            return Skill.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown skill: " + raw + " (expected reading|listening|writing)");
        }
    }
}
