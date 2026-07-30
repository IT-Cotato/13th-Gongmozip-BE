package org.cotato.gongmozip.global.ai.dto;

public record ProjectEvaluationResult(int score, String feedback) {
    public ProjectEvaluationResult {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
    }
}
