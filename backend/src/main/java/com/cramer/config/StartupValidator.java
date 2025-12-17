package com.cramer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Validates required configuration properties at application startup.
 * Ensures critical security settings are properly configured before the application runs.
 */
@Component
public class StartupValidator {

    private static final Logger logger = LoggerFactory.getLogger(StartupValidator.class);

    @Value("${supabase.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    /**
     * Validates critical configuration properties at startup.
     * The application will fail to start if required security properties are missing or invalid.
     */
    @Bean
    ApplicationRunner validateConfiguration() {
        return args -> {
            logger.info("🔒 Validating startup configuration...");

            // Validate JWT Secret
            if (jwtSecret == null || jwtSecret.isBlank()) {
                logger.error("❌ SUPABASE_JWT_SECRET environment variable is required but not set");
                throw new IllegalStateException(
                    "SUPABASE_JWT_SECRET environment variable is required but not set. " +
                    "Please set it in your .env file or environment variables."
                );
            }

            if (jwtSecret.length() < 32) {
                logger.error("❌ SUPABASE_JWT_SECRET is too short ({} characters, minimum 32 required)", jwtSecret.length());
                throw new IllegalStateException(
                    "SUPABASE_JWT_SECRET must be at least 32 characters for security. " +
                    "Current length: " + jwtSecret.length()
                );
            }

            // Validate Datasource URL (optional but log warning if not set)
            if (datasourceUrl == null || datasourceUrl.isBlank()) {
                logger.warn("⚠️ SPRING_DATASOURCE_URL is not set - database features will be unavailable");
            } else {
                logger.info("✅ Database connection configured");
            }

            logger.info("✅ JWT Secret validated (length: {} characters)", jwtSecret.length());
            logger.info("✅ Startup configuration validation complete");
        };
    }
}
