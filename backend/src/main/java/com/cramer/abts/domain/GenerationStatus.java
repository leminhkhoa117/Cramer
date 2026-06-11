package com.cramer.abts.domain;

/**
 * Outcome of an ABTS generation (SPEC-21 §8).
 */
public enum GenerationStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    NOT_IMPLEMENTED
}
