package com.cramer.abts.service;

import com.cramer.platform.common.json.Json;
import com.cramer.platform.integration.openrouter.OpenRouterClient;
import com.cramer.platform.integration.openrouter.OpenRouterError;
import com.cramer.platform.integration.openrouter.OpenRouterException;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelCatalogServiceTest {

    private final OpenRouterClient client = mock(OpenRouterClient.class);
    private final OpenRouterProperties props =
            new OpenRouterProperties("key", null, "deepseek/deepseek-v4-flash", 120_000, null, null);
    private final ModelCatalogService catalog =
            new ModelCatalogService(client, new ModelCapabilityRegistry(), props);

    @Test
    void usesCuratedFallbackWhenUpstreamFails() {
        when(client.listModels()).thenThrow(new OpenRouterException(OpenRouterError.UPSTREAM_ERROR, "down"));

        ArrayNode models = catalog.listModels();

        assertThat(models).isNotEmpty();
        assertThat(models.toString()).contains("deepseek/deepseek-v4-flash");
    }

    @Test
    void enrichesAndCachesLiveCatalog() {
        ArrayNode raw = Json.mapper().createArrayNode();
        raw.addObject().put("id", "openai/gpt-5").put("context_length", 400000);
        when(client.listModels()).thenReturn(raw);

        ArrayNode first = catalog.listModels();
        ArrayNode second = catalog.listModels();

        assertThat(first.get(0).path("id").asText()).isEqualTo("openai/gpt-5");
        assertThat(first.get(0).path("supportsReasoning").asBoolean()).isTrue();
        // cached for 5 minutes → upstream queried only once
        verify(client, times(1)).listModels();
        assertThat(second).isSameAs(first);
    }
}
