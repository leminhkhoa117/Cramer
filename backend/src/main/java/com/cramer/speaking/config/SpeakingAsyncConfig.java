package com.cramer.speaking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Bounded executor for asynchronous Speaking grading (SPEC-14 §6). Keeps grading off the request
 * thread; a small bounded queue applies back-pressure.
 */
@Configuration
public class SpeakingAsyncConfig {

    @Bean(name = "speakingGradingExecutor")
    public Executor speakingGradingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("speaking-grade-");
        executor.initialize();
        return executor;
    }
}
