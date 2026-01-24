package com.cramer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Base controller class providing common utility methods.
 * All REST controllers should extend this class to inherit shared functionality.
 *
 * @author Cramer Backend Team
 * @since 2026-01-24
 */
public abstract class BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    /**
     * Extract authenticated user's UUID from Spring Security context.
     *
     * @param authentication Spring Security authentication object
     * @return user's UUID
     * @throws IllegalArgumentException if authentication is null, name is null, or UUID format is invalid
     */
    protected UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null) {
            logger.error("Authentication object is null");
            throw new IllegalArgumentException("Authentication is required");
        }

        if (authentication.getName() == null) {
            logger.error("Authentication name (user ID) is null");
            throw new IllegalArgumentException("User ID is missing from authentication");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid UUID format in authentication: {}", authentication.getName());
            throw new IllegalArgumentException("Invalid user ID format: " + e.getMessage(), e);
        }
    }
}
