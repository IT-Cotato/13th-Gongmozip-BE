package org.cotato.gongmozip.domains.matching.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingResultStatus;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;

public final class MatchingResultResponse {

    private MatchingResultResponse() {}

    public record TodayMatchingResultResponse(
            MatchingResultStatus resultStatus,
            Long applicationId,
            LocalDate applicationDate,
            MatchingApplicationStatus applicationStatus,
            LocalDateTime publishedAt,
            InterestCategory contestCategory,
            Long matchingGroupId,
            Integer teamSize,
            BigDecimal matchingScore,
            MatchingScoreBreakdown scoreBreakdown,
            List<MatchingResultMemberResponse> members,
            LocalDateTime responseDeadlineAt,
            MatchingGroupStatus groupStatus,
            MatchingGroupMemberStatus myResponseStatus,
            Integer confirmedTeamSize,
            Long teamId) {

        public TodayMatchingResultResponse {
            members = members == null ? List.of() : List.copyOf(members);
        }
    }

    public record MatchingScoreBreakdown(
            BigDecimal leaderHarmonyScore,
            BigDecimal goalSimilarityScore,
            BigDecimal workStyleSimilarityScore,
            BigDecimal communicationSimilarityScore,
            BigDecimal agreeablenessSimilarityScore,
            BigDecimal conscientiousnessSimilarityScore,
            BigDecimal honestyHumilitySimilarityScore,
            BigDecimal extroversionComplementScore) {}

    public record MatchingResultMemberResponse(
            Long memberId,
            Long profileId,
            String nickname,
            CharacterType characterType,
            LeaderPreference leaderPreference,
            MatchingGroupMemberStatus responseStatus,
            boolean me) {}
}
