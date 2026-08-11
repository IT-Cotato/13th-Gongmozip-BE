package org.cotato.gongmozip.domains.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(
            name = "TeamCreationRequest",
            description = "QA용 팀 즉시 생성 요청(POST /api/test/teams). leaderPreference 조합으로 팀장 선출 시나리오를 직접 고를 수 있다 "
                    + "— WANTS 정확히 1명이면 AUTO_ASSIGNED, WANTS 2명 이상이면 CANDIDATE_VOTE, WANTS 0명이면 OPEN_NOMINATION.")
    public record TeamCreationRequest(
            @NotEmpty(message = "팀원 목록은 비어있을 수 없습니다.") @Valid List<TeamMemberInput> members,
            @NotNull(message = "관심 카테고리는 필수 입력 항목입니다.")
                    @Schema(description = "팀이 매칭받은 공모전 관심 카테고리", example = "PHOTO_VIDEO")
                    InterestCategory preferredCategory) {}

    // leaderPreference/extroversionType/extroversionScore는 매칭 신청(MatchingApplication)
    // 시점 스냅샷을 그대로 전달받아 TeamMember에 옮겨 적는다 (팀장 선출/추천 알고리즘 입력값,
    // docs/decisions/02-leader-election.md 참고).
    @Schema(name = "TeamMemberInput", description = "팀원 한 명의 매칭 신청 시점 스냅샷")
    public record TeamMemberInput(
            @NotNull(message = "memberId는 필수 입력 항목입니다.")
                    @Schema(description = "회원 ID (quick-login 응답 또는 실제 가입 계정)", example = "6")
                    Long memberId,
            @NotNull(message = "profileId는 필수 입력 항목입니다.") @Schema(description = "이 팀에서 사용할 프로필 ID", example = "11")
                    Long profileId,
            @NotNull(message = "leaderPreference는 필수 입력 항목입니다.")
                    @Schema(description = "팀장 희망 여부 — 팀 전체에서 WANTS 인원 수로 팀장 선출 모드가 정해진다", example = "WANTS")
                    LeaderPreference leaderPreference,
            @NotNull(message = "extroversionType은 필수 입력 항목입니다.") @Schema(description = "내향/앰비버트/외향 성향", example = "E")
                    ExtroversionType extroversionType,
            @NotNull(message = "extroversionScore는 필수 입력 항목입니다.")
                    @Schema(description = "외향성 점수 (1.0~5.0)", example = "4.2")
                    BigDecimal extroversionScore) {}

    public record ChatbotToggleRequest(boolean enabled) {}

    public record LeaderCandidacyRequest(boolean wants) {}

    public record LeaderVoteRequest(Long candidateTeamMemberId) {}

    public record UpdateProgressRequest(
            @NotNull(message = "진행률은 필수 입력 항목입니다.") @Min(0) @Max(100) Integer progressPercent) {}

    public record SubmitCompletionRequest(boolean completed) {}
}
