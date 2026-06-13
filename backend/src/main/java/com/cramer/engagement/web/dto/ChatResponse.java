package com.cramer.engagement.web.dto;

/** Chat reply (SPEC-16 §2). {@code remaining} = monthly allowance left (−1 unlimited). */
public record ChatResponse(String reply, int remaining) {
}
