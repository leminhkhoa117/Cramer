package com.cramer.controller;

import com.cramer.dto.GradingStatusDTO;
import com.cramer.dto.SubscriptionStatusDTO;
import com.cramer.dto.SubscriptionTierDTO;
import com.cramer.dto.UserSubscriptionDTO;
import com.cramer.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Subscription management.
 * Provides endpoints for subscription tiers, user subscriptions, and AI grading status.
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscription Management", description = "APIs for managing user subscriptions and AI grading limits")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(
        summary = "Get all subscription tiers",
        description = "Retrieve a list of all available subscription tiers (Cramerie, Cramerich, Cramerous)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tier list")
    })
    @GetMapping("/tiers")
    public ResponseEntity<List<SubscriptionTierDTO>> getAllTiers() {
        logger.info("📋 GET /api/subscriptions/tiers - Fetching all tiers");
        List<SubscriptionTierDTO> tiers = subscriptionService.getAllTiers();
        return ResponseEntity.ok(tiers);
    }

    @Operation(
        summary = "Get tier by code",
        description = "Retrieve a specific subscription tier by its code"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tier found"),
        @ApiResponse(responseCode = "404", description = "Tier not found")
    })
    @GetMapping("/tiers/{code}")
    public ResponseEntity<SubscriptionTierDTO> getTierByCode(@PathVariable String code) {
        logger.info("🔍 GET /api/subscriptions/tiers/{} - Fetching tier", code);
        SubscriptionTierDTO tier = subscriptionService.getTierByCode(code);
        return ResponseEntity.ok(tier);
    }

    @Operation(
        summary = "Get current user's subscription",
        description = "Retrieve the authenticated user's current subscription details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription found or created"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/current")
    public ResponseEntity<UserSubscriptionDTO> getCurrentSubscription(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("👤 GET /api/subscriptions/current - User: {}", userId);
        
        UserSubscriptionDTO subscription = subscriptionService.getUserSubscription(userId);
        return ResponseEntity.ok(subscription);
    }

    @Operation(
        summary = "Check AI grading availability",
        description = "Check if the user can use AI grading based on subscription limits and Lúa balance"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grading status returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/grading-status")
    public ResponseEntity<GradingStatusDTO> getGradingStatus(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("🔍 GET /api/subscriptions/grading-status - User: {}", userId);
        
        GradingStatusDTO status = subscriptionService.checkAIGradingAllowed(userId);
        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "Get remaining AI gradings",
        description = "Get the number of AI gradings remaining for this billing period"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Remaining count returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/gradings-remaining")
    public ResponseEntity<Integer> getGradingsRemaining(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("🔢 GET /api/subscriptions/gradings-remaining - User: {}", userId);
        
        int remaining = subscriptionService.getMonthlyGradingsRemaining(userId);
        return ResponseEntity.ok(remaining);
    }

    @Operation(
        summary = "Get daily chat limit",
        description = "Get the user's daily chat message limit based on subscription tier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Daily limit returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/chat-limit")
    public ResponseEntity<Integer> getChatLimit(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("💬 GET /api/subscriptions/chat-limit - User: {}", userId);
        
        int limit = subscriptionService.getDailyChatLimit(userId);
        return ResponseEntity.ok(limit);
    }

    @Operation(
        summary = "Get comprehensive subscription status",
        description = "Retrieve complete subscription status including tier info, usage stats, credits, and payment history"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved subscription status"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-status")
    public ResponseEntity<SubscriptionStatusDTO> getMyStatus(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        logger.info("📊 GET /api/subscriptions/my-status - User: {}", userId);
        
        SubscriptionStatusDTO status = subscriptionService.getSubscriptionStatus(userId);
        return ResponseEntity.ok(status);
    }

    @Operation(
        summary = "Toggle AI grading preference",
        description = "Enable or disable AI grading (ATTEMPT_AI) for the user. Only Cramerich+ users can enable this."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI grading preference updated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Cramerie users cannot enable AI grading")
    })
    @PutMapping("/ai-grading")
    public ResponseEntity<?> toggleAiGrading(
            Authentication authentication,
            @RequestBody java.util.Map<String, Boolean> request) {
        UUID userId = UUID.fromString(authentication.getName());
        Boolean enabled = request.get("enabled");
        
        logger.info("🔄 PUT /api/subscriptions/ai-grading - User: {}, Enabled: {}", userId, enabled);
        
        if (enabled == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", "Missing 'enabled' field in request body"
            ));
        }
        
        try {
            boolean result = subscriptionService.setAiGradingEnabled(userId, enabled);
            return ResponseEntity.ok(java.util.Map.of(
                "aiGradingEnabled", result,
                "message", result ? "Đã bật Lượt chấm nâng cao" : "Đã tắt Lượt chấm nâng cao"
            ));
        } catch (IllegalStateException e) {
            // Cramerie user trying to enable AI grading
            return ResponseEntity.status(403).body(java.util.Map.of(
                "error", e.getMessage(),
                "canEnable", false
            ));
        }
    }
}
