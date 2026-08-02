package org.cotato.gongmozip.domains.matching.converter;

import java.util.List;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationDetailResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingExplanationResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonDetailResponse;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.entity.MatchingExplanation;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.cotato.gongmozip.domains.member.entity.Member;

public class MatchingConverter {

    public static MatchingExplanationResponse toExplanationResponse(MatchingExplanation explanation) {
        return new MatchingExplanationResponse(
                explanation.getTitle(),
                explanation.getSummary(),
                explanation.getSections(),
                explanation.getDisclaimer(),
                explanation.getUpdatedAt());
    }

    public static MatchingReasonCreateResponse toReasonCreateResponse(MatchingReason reason) {
        return new MatchingReasonCreateResponse(
                reason.getMatchingReasonId(),
                reason.getMatchingGroup().getMatchingGroupId(),
                reason.getStatus().name(),
                reason.getCreatedAt());
    }

    public static MatchingReasonDetailResponse toReasonDetailResponse(MatchingReason reason) {
        return new MatchingReasonDetailResponse(
                reason.getMatchingReasonId(),
                reason.getMatchingGroup().getMatchingGroupId(),
                reason.getStatus().name(),
                reason.getHeadline(),
                reason.getSummary(),
                reason.getStrengths(),
                reason.getCommonPoints(),
                reason.getComplementaryPoints(),
                reason.getCautions(),
                reason.getTotalCompatibilityScore(),
                reason.getTeamGoalScore(),
                reason.getPersonalityScore(),
                reason.getExtraversionComplementScore(),
                reason.getFailureMessage(),
                reason.getEvaluatedAt(),
                reason.getCreatedAt());
    }

    public static LeaderRecommendationCreateResponse toLeaderRecCreateResponse(LeaderRecommendation rec) {
        return new LeaderRecommendationCreateResponse(
                rec.getLeaderRecommendationId(),
                rec.getTeam().getTeamId(),
                rec.getStatus().name(),
                rec.getCreatedAt());
    }

    public static LeaderRecommendationDetailResponse toLeaderRecDetailResponse(
            LeaderRecommendation rec, String recommendedMemberNickname, List<LeaderCandidateResponse> candidates) {
        Member leader = rec.getRecommendedMember();
        Long leaderId = (leader != null) ? leader.getMemberId() : null;

        return new LeaderRecommendationDetailResponse(
                rec.getLeaderRecommendationId(),
                rec.getTeam().getTeamId(),
                rec.getStatus().name(),
                leaderId,
                recommendedMemberNickname,
                rec.getRecommendationReason(),
                candidates,
                rec.getTeamSummary(),
                rec.getCaution(),
                rec.getFailureMessage(),
                rec.getEvaluatedAt(),
                rec.getCreatedAt());
    }
}
