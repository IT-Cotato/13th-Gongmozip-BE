package org.cotato.gongmozip.domains.matching.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MatchingResponse {

    public record MatchingExplanationResponse(
            String title, String summary, List<SectionResponse> sections, String disclaimer, LocalDateTime updatedAt) {}

    public record SectionResponse(String type, String title, String description, List<ItemResponse> items) {}

    public record ItemResponse(String title, String description) {}

    public record MatchingReasonCreateResponse(
            Long reasonId, Long matchingResultId, String status, LocalDateTime createdAt) {}

    public record MatchingReasonDetailResponse(
            Long reasonId,
            Long matchingResultId,
            String status,
            String headline,
            String summary,
            List<TitleDescriptionInfo> strengths,
            List<String> commonPoints,
            List<TitleDescriptionInfo> complementaryPoints,
            List<String> cautions,
            Integer totalCompatibilityScore,
            Integer teamGoalScore,
            Integer personalityScore,
            Integer extraversionComplementScore,
            String failureMessage,
            LocalDateTime generatedAt,
            LocalDateTime createdAt) {}

    public record TitleDescriptionInfo(String title, String description) {}

    public record LeaderRecommendationCreateResponse(
            Long recommendationId, Long teamId, String status, LocalDateTime createdAt) {}

    public record LeaderRecommendationDetailResponse(
            Long recommendationId,
            Long teamId,
            String status,
            Long recommendedMemberId,
            String recommendedMemberNickname,
            String recommendationReason,
            List<LeaderCandidateResponse> candidates,
            String teamSummary,
            String caution,
            String failureMessage,
            LocalDateTime generatedAt,
            LocalDateTime createdAt) {}

    public record LeaderCandidateResponse(Long memberId, String nickname, Integer rank, Integer score, String reason) {}
}
