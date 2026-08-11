package org.cotato.gongmozip.domains.team.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;

public final class TeamRequest {

    private TeamRequest() {}

    /**
     * 매칭 도메인이 팀을 생성할 때 넘겨야 하는 입력 계약. 일반 매칭 컨트롤러에는 노출되지 않고
     * 매칭 서비스가 TeamService를 직접 호출하는 내부 계약이지만, QA용 TeamTestController
     * (`POST /api/test/teams`, X-Test-Api-Key 헤더로 별도 인가)에서는 HTTP로도 받는다.
     * (docs/decisions/01-team.md 참고)
     */
    public record TeamCreationRequest(
            @NotEmpty(message = "팀원 목록은 비어있을 수 없습니다.") @Valid List<TeamMemberInput> members,
            @NotNull(message = "관심 카테고리는 필수 입력 항목입니다.") InterestCategory preferredCategory) {}

    // leaderPreference/extroversionType/extroversionScore는 매칭 신청(MatchingApplication)
    // 시점 스냅샷을 그대로 전달받아 TeamMember에 옮겨 적는다 (팀장 선출/추천 알고리즘 입력값,
    // docs/decisions/02-leader-election.md 참고).
    public record TeamMemberInput(
            @NotNull(message = "memberId는 필수 입력 항목입니다.") Long memberId,
            @NotNull(message = "profileId는 필수 입력 항목입니다.") Long profileId,
            @NotNull(message = "leaderPreference는 필수 입력 항목입니다.") LeaderPreference leaderPreference,
            @NotNull(message = "extroversionType은 필수 입력 항목입니다.") ExtroversionType extroversionType,
            @NotNull(message = "extroversionScore는 필수 입력 항목입니다.") BigDecimal extroversionScore) {}

    public record ChatbotToggleRequest(boolean enabled) {}

    public record LeaderCandidacyRequest(boolean wants) {}

    public record LeaderVoteRequest(Long candidateTeamMemberId) {}

    public record UpdateProgressRequest(
            @NotNull(message = "진행률은 필수 입력 항목입니다.") @Min(0) @Max(100) Integer progressPercent) {}

    public record SubmitCompletionRequest(boolean completed) {}
}
