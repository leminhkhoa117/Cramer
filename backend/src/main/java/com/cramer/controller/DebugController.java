package com.cramer.controller;

import com.cramer.entity.CreditTransaction;
import com.cramer.entity.SubscriptionTier;
import com.cramer.entity.UserSubscription;
import com.cramer.repository.SubscriptionTierRepository;
import com.cramer.repository.UserSubscriptionRepository;
import com.cramer.service.CreditService;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Debug Controller - ONLY for development/testing purposes.
 * 
 * SECURITY MEASURES:
 * 1. Only enabled when DEBUG_ENABLED=true environment variable is set
 * 2. Requires DEBUG_SECRET_KEY header for authentication
 * 3. All operations are logged for audit trail
 * 
 * In production: DO NOT set DEBUG_ENABLED=true or DEBUG_SECRET_KEY
 * This will cause all endpoints to return 404 Not Found.
 */
@RestController
@RequestMapping("/api/debug")
@Hidden // Hide from Swagger in production
public class DebugController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @Value("${debug.enabled:false}")
    private boolean debugEnabled;

    @Value("${debug.secret-key:}")
    private String debugSecretKey;

    private final SubscriptionTierRepository tierRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final CreditService creditService;

    @Autowired
    public DebugController(
            SubscriptionTierRepository tierRepository,
            UserSubscriptionRepository subscriptionRepository,
            CreditService creditService) {
        this.tierRepository = tierRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
    }

    /**
     * Check if debug mode is enabled and secret key is valid.
     */
    private boolean isAuthorized(String providedKey) {
        if (!debugEnabled) {
            logger.warn("⚠️ Debug endpoint called but DEBUG_ENABLED=false");
            return false;
        }
        if (debugSecretKey == null || debugSecretKey.isEmpty()) {
            logger.warn("⚠️ Debug endpoint called but DEBUG_SECRET_KEY not set");
            return false;
        }
        if (!debugSecretKey.equals(providedKey)) {
            logger.warn("⚠️ Debug endpoint called with invalid secret key");
            return false;
        }
        return true;
    }

    /**
     * Activate/upgrade subscription for the authenticated user.
     * 
     * Usage: POST /api/debug/activate-subscription
     * Headers: X-Debug-Key: your-secret-key
     * Body: { "tierCode": "cramerich" }
     */
    @PostMapping("/activate-subscription")
    @Transactional
    public ResponseEntity<Map<String, Object>> activateSubscription(
            Authentication authentication,
            @RequestHeader(value = "X-Debug-Key", required = false) String debugKey,
            @RequestBody Map<String, String> request) {

        if (!isAuthorized(debugKey)) {
            return ResponseEntity.notFound().build();
        }

        UUID userId = getCurrentUserId(authentication);
        String tierCode = request.getOrDefault("tierCode", "cramerich");

        logger.warn("🔧 DEBUG: Activating subscription for user {} to tier {}", userId, tierCode);

        // Find the tier
        SubscriptionTier tier = tierRepository.findByCode(tierCode).orElse(null);
        if (tier == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Tier not found: " + tierCode
            ));
        }

        // Get or create subscription
        UserSubscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElse(null);

        if (subscription == null) {
            subscription = UserSubscription.builder()
                    .userId(userId)
                    .tier(tier)
                    .status(UserSubscription.Status.ACTIVE)
                    .attemptAisUsed(0)
                    .paymentReference("DEBUG_" + System.currentTimeMillis())
                    .autoRenew(false)
                    .build();
        } else {
            subscription.setTier(tier);
            subscription.setPaymentReference("DEBUG_" + System.currentTimeMillis());
            subscription.setAttemptAisUsed(0);
        }

        // Set expiration (1 month from now)
        subscription.setExpiresAt(OffsetDateTime.now().plusMonths(1));
        subscriptionRepository.save(subscription);

        // Add tier bonus Lúa if applicable
        if (tier.getInitialLua() != null && tier.getInitialLua() > 0) {
            creditService.earnCredits(
                    userId,
                    tier.getInitialLua(),
                    CreditTransaction.Category.TIER_BONUS,
                    "[DEBUG] Thưởng nâng cấp gói " + tier.getName(),
                    "DEBUG"
            );
        }

        logger.warn("✅ DEBUG: Subscription activated - User: {}, Tier: {}, Expires: {}",
                userId, tierCode, subscription.getExpiresAt());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Subscription activated successfully (DEBUG MODE)",
                "tier", tierCode,
                "expiresAt", subscription.getExpiresAt().toString(),
                "userId", userId.toString()
        ));
    }

    /**
     * Add Lúa credits to the authenticated user.
     * 
     * Usage: POST /api/debug/add-lua
     * Headers: X-Debug-Key: your-secret-key
     * Body: { "amount": 100 }
     */
    @PostMapping("/add-lua")
    @Transactional
    public ResponseEntity<Map<String, Object>> addLua(
            Authentication authentication,
            @RequestHeader(value = "X-Debug-Key", required = false) String debugKey,
            @RequestBody @jakarta.validation.Valid Map<String, @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(10000) Integer> request) {

        if (!isAuthorized(debugKey)) {
            return ResponseEntity.notFound().build();
        }

        UUID userId = getCurrentUserId(authentication);
        Integer amount = request.getOrDefault("amount", 100);

        if (amount <= 0 || amount > 10000) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Amount must be between 1 and 10000"
            ));
        }

        logger.warn("🔧 DEBUG: Adding {} Lúa to user {}", amount, userId);

        creditService.earnCredits(
                userId,
                amount,
                CreditTransaction.Category.TIER_BONUS,
                "[DEBUG] Test credits",
                "DEBUG"
        );

        logger.warn("✅ DEBUG: Added {} Lúa to user {}", amount, userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lúa added successfully (DEBUG MODE)",
                "amount", amount,
                "userId", userId.toString()
        ));
    }

    /**
     * Check debug mode status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @RequestHeader(value = "X-Debug-Key", required = false) String debugKey) {

        if (!isAuthorized(debugKey)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "debugEnabled", true,
                "message", "Debug mode is active. Use with caution!"
        ));
    }
}
