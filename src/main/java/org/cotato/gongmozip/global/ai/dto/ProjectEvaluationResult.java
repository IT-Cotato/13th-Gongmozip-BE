package org.cotato.gongmozip.global.ai.dto;

public record ProjectEvaluationResult(
        int score,
        Integer rScore,
        Integer oScore,
        Integer fScore,
        boolean injectionDetected,
        boolean insufficientInput,
        String feedback,
        String summary) {
    public ProjectEvaluationResult {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        if (rScore != null && (rScore < 0 || rScore > 5)) {
            throw new IllegalArgumentException("rScore must be between 0 and 5");
        }
        if (oScore != null && (oScore < 0 || oScore > 5)) {
            throw new IllegalArgumentException("oScore must be between 0 and 5");
        }
        if (fScore != null && (fScore < 0 || fScore > 5)) {
            throw new IllegalArgumentException("fScore must be between 0 and 5");
        }
    }
}
