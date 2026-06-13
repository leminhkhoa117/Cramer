package com.cramer.writing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Bounded executor for asynchronous writing grading (SPEC-04 §5, SPEC-13 §4). Module-local pool
 * so a grading surge cannot starve other async work. Queue saturation surfaces as a rejected
 * execution (caller-runs) rather than a silent drop.
 */
@Configuration
public class WritingAsyncConfig {

    public static final String EXECUTOR = "writingGradingExecutor";

    @Bean(EXECUTOR)
    public Executor writingGradingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("writing-grade-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
