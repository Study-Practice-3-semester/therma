package com.evraz.therma.dto;

import java.time.Instant;
import java.util.List;

public record Recommendation(
    Long id,
    String cowperId,
    Instant createdAt,
    String ruleCode,
    String message,
    Priority priority,
    RecommendationStatus status,
    ExpectedEffect expectedEffect,
    List<String> reasoning,
    String targetParameter,
    Double suggestedValueMin,
    Double suggestedValueMax
) {
    public Recommendation withReasoning(List<String> newReasoning) {
        return new Recommendation(
            id, cowperId, createdAt, ruleCode, message, priority, status,
            expectedEffect, newReasoning, targetParameter, suggestedValueMin, suggestedValueMax
        );
    }

    public Recommendation withId(Long newId) {
        return new Recommendation(
            newId, cowperId, createdAt, ruleCode, message, priority, status,
            expectedEffect, reasoning, targetParameter, suggestedValueMin, suggestedValueMax
        );
    }
}
