package com.cramer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SpeakingGradingAsyncConfig {

    @Value("${speaking.grading.executor.core-size:2}")
    private int coreSize;

    @Value("${speaking.grading.executor.max-size:4}")
    private int maxSize;

    @Value("${speaking.grading.executor.queue-capacity:50}")
    private int queueCapacity;

    @Bean("speakingGradingExecutor")
    public ThreadPoolTaskExecutor speakingGradingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("speaking-grading-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
