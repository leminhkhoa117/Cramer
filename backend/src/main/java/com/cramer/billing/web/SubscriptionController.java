package com.cramer.billing.web;

import com.cramer.billing.domain.SubscriptionTier;
import com.cramer.billing.domain.UserSubscription;
import com.cramer.billing.service.SubscriptionService;
import com.cramer.billing.web.dto.BillingRequests;
import com.cramer.billing.web.dto.SubscriptionStatusView;
import com.cramer.billing.web.dto.TierView;
import com.cramer.platform.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription endpoints (SPEC-15 §9). Tiers are public catalog data; status/actions are scoped
 * to the authenticated user (from {@link CurrentUser}).
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptions;
    private final CurrentUser currentUser;

    public SubscriptionController(SubscriptionService subscriptions, CurrentUser currentUser) {
        this.subscriptions = subscriptions;
        this.currentUser = currentUser;
    }

    @GetMapping("/tiers")
    public List<TierView> tiers() {
        return subscriptions.listTiers().stream().map(this::toTierView).toList();
    }

    @GetMapping("/tiers/{code}")
    public TierView tier(@PathVariable String code) {
        return toTierView(subscriptions.getTierByCode(code));
    }

    @GetMapping("/current")
    public SubscriptionStatusView current() {
        return status(currentUser.requireUserId());
    }

    @GetMapping("/my-status")
    public SubscriptionStatusView myStatus() {
        return status(currentUser.requireUserId());
    }

    @GetMapping("/grading-status")
    public Map<String, Object> gradingStatus() {
        UUID userId = currentUser.requireUserId();
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        return Map.of(
                "aiGradingEnabled", Boolean.TRUE.equals(sub.getAiGradingEnabled()),
                "gradingsRemaining", subscriptions.gradingsRemaining(userId));
    }

    @GetMapping("/gradings-remaining")
    public Map<String, Object> gradingsRemaining() {
        return Map.of("remaining", subscriptions.gradingsRemaining(currentUser.requireUserId()));
    }

    @GetMapping("/chat-limit")
    public Map<String, Object> chatLimit() {
        return Map.of("limit", subscriptions.chatLimit(currentUser.requireUserId()));
    }

    @PutMapping("/ai-grading")
    public SubscriptionStatusView setAiGrading(@RequestBody BillingRequests.SetAiGrading request) {
        UUID userId = currentUser.requireUserId();
        subscriptions.setAiGrading(userId, request.enabled());
        return status(userId);
    }

    // ---- helpers ----

    private SubscriptionStatusView status(UUID userId) {
        UserSubscription sub = subscriptions.getOrCreateActive(userId);
        SubscriptionTier tier = subscriptions.tierOf(sub);
        return new SubscriptionStatusView(
                tier.getCode(), tier.getName(), tier.isPremium(), sub.getStatus(), sub.getExpiresAt(),
                Boolean.TRUE.equals(sub.getAutoRenew()), sub.getAttemptsUsed(), sub.getAttemptAisUsed(),
                sub.getChatbotUsed(), Boolean.TRUE.equals(sub.getAiGradingEnabled()),
                subscriptions.gradingsRemaining(userId), tier.getChatbotMonthlyLimit());
    }

    private TierView toTierView(SubscriptionTier t) {
        return new TierView(t.getId(), t.getCode(), t.getName(), t.getPriceVnd(), t.isPremium(),
                t.getMonthlyAttemptLimit(), t.getMonthlyAttemptAiLimit(), t.getPerSkillAttemptLimit(),
                t.getIncludedAiGradings(), t.getChatbotMonthlyLimit(), t.getMonthlyTranslationLimit(),
                t.getInitialLua(), t.getMonthlyLuaBonus() == null ? 0 : t.getMonthlyLuaBonus(), t.getFeatures());
    }
}
