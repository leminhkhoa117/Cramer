package com.cramer.config;

import com.cramer.service.SpeakingSelectionPlannerService;
import com.cramer.service.implement.HeuristicSpeakingSelectionPlannerService;
import com.cramer.service.implement.LlmSpeakingSelectionPlannerService;
import com.cramer.service.speaking.SpeakingSelectionPromptBuilder;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Wires the {@link SpeakingSelectionPlannerService} bean based on configuration.
 *
 * <ul>
 *   <li>{@code speaking.selection.provider=llm} → {@link LlmSpeakingSelectionPlannerService}
 *       (with {@link HeuristicSpeakingSelectionPlannerService} as internal fallback)</li>
 *   <li>Any other value or missing → {@link HeuristicSpeakingSelectionPlannerService}
 *       via {@link ConditionalOnMissingBean}</li>
 * </ul>
 *
 * @since 2026-04-05
 */
@Configuration
public class SpeakingSelectionPlannerConfig {

    @Bean
    @ConditionalOnProperty(name = "speaking.selection.provider", havingValue = "llm")
    public SpeakingSelectionPlannerService llmSpeakingSelectionPlanner(
            OpenRouterConfig openRouterConfig,
            SpeakingSelectionProperties selectionProperties,
            SpeakingSelectionPromptBuilder promptBuilder,
            HeuristicSpeakingSelectionPlannerService heuristicFallback) {
        RestTemplate selectionRestTemplate = buildSelectionRestTemplate(selectionProperties);
        return new LlmSpeakingSelectionPlannerService(
                openRouterConfig, selectionProperties, promptBuilder,
                heuristicFallback, selectionRestTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(SpeakingSelectionPlannerService.class)
    public HeuristicSpeakingSelectionPlannerService heuristicSpeakingSelectionPlanner() {
        return new HeuristicSpeakingSelectionPlannerService();
    }

    private RestTemplate buildSelectionRestTemplate(SpeakingSelectionProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(5000));
        factory.setReadTimeout(Duration.ofMillis(props.getTimeoutMs()));
        return new RestTemplate(factory);
    }
}
