package com.cramer.billing.service;

import java.util.UUID;

/**
 * Published billing contract for AI vocabulary translation, charged <strong>after success</strong>
 * (SPEC-04 §4, SPEC-15 §6, category {@code VOCABULARY_TRANSLATION}). Consumed by {@code engagement}.
 */
public interface TranslationBillingPort {

    /** Pre-flight check (no charge): may this user translate now? */
    boolean canTranslate(UUID userId);

    /** Account for one translation after success (counter++ or overage charge). */
    void chargeTranslation(UUID userId, String reference);
}
