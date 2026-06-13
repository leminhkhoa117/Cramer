package com.cramer.billing.service;

import java.util.UUID;

/**
 * Published billing contract for AI chat, charged <strong>after a successful reply</strong>
 * (SPEC-04 §4, SPEC-15 §6). Consumed by {@code engagement}. The monthly subscription counter is
 * the source of truth (not the legacy daily {@code chatbot_usage} table).
 */
public interface ChatBillingPort {

    /** Pre-flight check (no charge): may this user send a chat message now? */
    boolean canChat(UUID userId);

    /** Remaining monthly chat allowance ({@code -1} = unlimited). */
    int remaining(UUID userId);

    /** Account for one chat message after the reply succeeds (counter++ or overage charge). */
    void chargeChat(UUID userId, String reference);
}
