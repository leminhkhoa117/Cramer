package com.cramer.billing.web.dto;

/**
 * Request bodies for billing actions (SPEC-15 §8, §9). Grouped to avoid DTO sprawl.
 */
public final class BillingRequests {

    private BillingRequests() {
    }

    /** Create a subscription payment order; requires a paid tier (free is rejected). */
    public record CreateSubscriptionOrder(Long tierId, String tierCode) {
    }

    /** Create a Lúa-pack payment order; requires a DB pack code. */
    public record CreateLuaOrder(String packCode) {
    }

    /** Toggle AI-grading on the active subscription (premium only). */
    public record SetAiGrading(boolean enabled) {
    }
}
