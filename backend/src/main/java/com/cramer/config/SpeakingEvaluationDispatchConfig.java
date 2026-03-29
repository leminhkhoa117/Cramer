package com.cramer.config;

import com.cramer.service.SpeakingEvaluationDispatchService;
import com.cramer.service.implement.NoopSpeakingEvaluationDispatchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpeakingEvaluationDispatchConfig {

    @Bean
    @ConditionalOnMissingBean(SpeakingEvaluationDispatchService.class)
    public SpeakingEvaluationDispatchService speakingEvaluationDispatchService() {
        return new NoopSpeakingEvaluationDispatchService();
    }
}
