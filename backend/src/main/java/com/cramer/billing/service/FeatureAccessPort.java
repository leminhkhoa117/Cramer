package com.cramer.billing.service;

import java.util.UUID;

/**
 * Published feature-gating contract (SPEC-04 §4, SPEC-15 §7). Consumers (catalog, engagement)
 * gate features by the user's active tier. <strong>Fix:</strong> this port is actually wired into
 * request paths — the old service existed but was never called.
 */
public interface FeatureAccessPort {

    /** Whether the user's active tier grants the named feature. */
    boolean hasFeature(UUID userId, String feature);

    /** Whether the user is on a premium (paid) tier. */
    boolean isPremium(UUID userId);
}
