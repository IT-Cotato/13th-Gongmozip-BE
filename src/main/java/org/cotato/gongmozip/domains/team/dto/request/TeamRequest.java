package org.cotato.gongmozip.domains.team.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

public final class TeamRequest {

    private TeamRequest() {}

    /**
     * 매칭 도메인이 팀을 생성할 때 넘겨야 하는 입력 계약. HTTP로 노출되지 않고
     * 매칭 서비스가 TeamService를 직접 호출하는 내부 계약이다.
     * (docs/decisions/01-team.md 참고)
     */
    public record TeamCreationRequest(List<TeamMemberInput> members, InterestCategory preferredCategory) {}

    // leaderPreference/extroversionType/extroversionScore는 매칭 신청(MatchingApplication)
    // 시점 스냅샷을 그대로 전달받아 TeamMember에 옮겨 적는다 (팀장 선출/추천 알고리즘 입력값,
    // docs/decisions/02-leader-election.md 참고).
    public record TeamMemberInput(
            Long memberId,
            Long profileId,
            LeaderPreference leaderPreference,
            ExtroversionType extroversionType,
            BigDecimal extroversionScore) {}

    public record ChatbotToggleRequest(boolean enabled) {}

    public record LeaderCandidacyRequest(boolean wants) {}

    public record LeaderVoteRequest(Long candidateTeamMemberId) {}

    public record UpdateProgressRequest(
            @NotNull(message = "진행률은 필수 입력 항목입니다.") @Min(0) @Max(100) Integer progressPercent) {}

    public record SubmitCompletionRequest(boolean completed) {}
}
