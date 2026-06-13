package com.cramer.abts.generation;

import com.cramer.abts.service.ModelCapabilityRegistry;
import com.cramer.abts.web.dto.ModelConfig;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

/**
 * Resolves the effective model slug (requested → configured default) and the reasoning payload for
 * a request (SPEC-21 §1, §7, SPEC-24 §1). Thin seam shared by the generation and refinement
 * services so model selection lives in one place.
 */
@Service
public class ModelResolver {

    private final OpenRouterProperties props;
    private final ModelCapabilityRegistry capabilities;

    public ModelResolver(OpenRouterProperties props, ModelCapabilityRegistry capabilities) {
        this.props = props;
        this.capabilities = capabilities;
    }

    public String resolve(String requested) {
        return (requested == null || requested.isBlank()) ? props.resolvedDefaultModel() : requested.trim();
    }

    public JsonNode reasoningPayload(String model, ModelConfig config) {
        return capabilities.reasoningPayload(model, config);
    }
}
