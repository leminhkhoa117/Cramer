package com.cramer.admin.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Published cross-module contract (SPEC-04 §4, SPEC-17) for writing the admin audit trail.
 * Consumed by speaking (regrade), billing, catalog. Records/primitives only across the boundary.
 */
public interface AuditPort {

    /**
     * Append an audit entry.
     *
     * @param adminUserId authenticated admin principal (attribution; never a header)
     * @param action      e.g. {@code SPEAKING_REGRADE}, {@code STATUS_CHANGE}, {@code CREDITS_ADD}
     * @param targetType  e.g. {@code USER}, {@code SPEAKING_SESSION}, {@code SUBSCRIPTION}
     * @param targetId    target entity id (string form)
     */
    void record(UUID adminUserId, String action, String targetType, String targetId,
                String description, JsonNode oldValue, JsonNode newValue);
}
