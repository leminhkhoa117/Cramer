package com.cramer.abts.domain;

/**
 * Token/cost accounting for a generation (SPEC-21 §8). Immutable; {@link #plus} aggregates
 * usage across phases/parts during a multi-part merge.
 */
public record TokenUsage(int promptTokens, int completionTokens, int totalTokens, double cost) {

    public static final TokenUsage ZERO = new TokenUsage(0, 0, 0, 0.0);

    public static TokenUsage of(Integer prompt, Integer completion, Integer total, Double cost) {
        int p = prompt == null ? 0 : prompt;
        int c = completion == null ? 0 : completion;
        int t = total == null ? (p + c) : total;
        return new TokenUsage(p, c, t, cost == null ? 0.0 : cost);
    }

    public TokenUsage plus(TokenUsage other) {
        if (other == null) {
            return this;
        }
        return new TokenUsage(
                promptTokens + other.promptTokens,
                completionTokens + other.completionTokens,
                totalTokens + other.totalTokens,
                cost + other.cost);
    }
}
