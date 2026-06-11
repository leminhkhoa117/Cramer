package com.cramer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cramer backend entry point. {@link ConfigurationPropertiesScan} registers every
 * {@code @ConfigurationProperties} record across the modules (Supabase, LLM, OpenRouter,
 * speaking session, ...). Async + scheduling are enabled for grading dispatch and resets.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class CramerBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CramerBackendApplication.class, args);
    }
}
