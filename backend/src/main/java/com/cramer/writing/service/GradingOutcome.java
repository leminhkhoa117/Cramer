package com.cramer.writing.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * Result of grading one essay (SPEC-13 §4.2). {@code overallBand} is the recomputed average of
 * the criterion bands (the model's own overall is ignored). {@code aiFeedback} holds the
 * non-band feedback fields.
 */
public record GradingOutcome(BigDecimal overallBand, JsonNode bandScores, JsonNode aiFeedback) {
}
