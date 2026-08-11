package org.cotato.gongmozip.domains.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamCreationRequest;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.cotato.gongmozip.global.exception.CustomException;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ 임시 개발용 엔드포인트. 실제 매칭 신청/수락 없이 지정한 memberId/profileId 조합으로
 * {@link TeamService#createTeam}을 그대로 호출해 팀(채팅방)을 만든다 — 인사 메시지 발행, 상태
 * 전이, 팀장 선출 모드 계산까지 실제 매칭으로 만든 팀과 완전히 동일하게 동작한다. QA가
 * AUTO_ASSIGNED/CANDIDATE_VOTE/OPEN_NOMINATION 등 특정 시나리오를 재현하려면 팀원별
 * leaderPreference를 원하는 대로 지정해서 요청하면 된다.
 *
 * <p>QA가 배포 서버에서도 써야 해서 {@code @Profile("local")}(배포 서버에서는 절대 안 켜짐,
 * api.md 참고)이 아니라, {@code .env}의 {@code TEST_API_KEY}와 일치하는 헤더가 있을 때만
 * 동작하는 이 엔드포인트 전용 게이트를 쓴다 — quick-login 등 다른 로컬 전용 엔드포인트까지
 * 한꺼번에 열리는 걸 막기 위함. 값이 비어있으면(기본값) 어떤 요청도 통과할 수 없다 — fail-safe.
 *
 * <p>키 검사가 요청 본문 역직렬화 이후(핸들러 진입 시점)에 일어나 키가 없는 요청도 JSON 파싱
 * 비용은 치른다 — Security filter/AuthorizationManager로 옮기면 막을 수 있지만, QA 종료 후
 * 삭제될 임시 엔드포인트라 그 정도 리팩터링 비용을 들이지 않기로 했다(CodeRabbit 리뷰,
 * PR #131). 키가 틀렸을 때 요청이 실행되는 건 여전히 막혀 있어 인가 자체는 안전하다.
 */
@Tag(name = "TeamTest", description = "[개발용] 실제 매칭 플로우와 동일하게 팀(채팅방)을 즉시 생성 - QA 시나리오 재현용")
@RestController
@RequestMapping("/api/test/teams")
@RequiredArgsConstructor
public class TeamTestController {

    private static final String API_KEY_HEADER = "X-Test-Api-Key";

    private final TeamService teamService;

    @Value("${test.api-key:}")
    private String testApiKey;

    @Operation(
            summary = "[개발용] 매칭 신청/수락 없이 팀(채팅방)을 즉시 생성",
            description = "실제 매칭 플로우와 동일하게 TeamService.createTeam을 호출한다. leaderPreference 조합으로 "
                    + "AUTO_ASSIGNED(WANTS 1명)/CANDIDATE_VOTE(WANTS 2명+)/OPEN_NOMINATION(WANTS 0명) 시나리오를 직접 고를 수 있다.")
    @PostMapping
    public TeamCreationTestResponse createTeam(
            @Parameter(
                            required = true,
                            description = "서버 .env의 TEST_API_KEY와 일치해야 하는 게이트 키. 값이 없거나 다르면 403.",
                            example = "gongmozip-qa-<발급받은 값>")
                    @RequestHeader(value = API_KEY_HEADER, required = false)
                    String apiKey,
            @RequestBody @Valid TeamCreationRequest request) {
        requireValidApiKey(apiKey);
        Team team = teamService.createTeam(request);
        return new TeamCreationTestResponse(
                team.getTeamId(),
                team.getStatus().name(),
                team.getLeaderSelectionMode().name());
    }

    // testApiKey가 비어있으면(.env에 TEST_API_KEY 미설정) 어떤 헤더값과도 매치될 수 없어
    // 항상 거부된다 - 명시적으로 켜기 전까지는 기본값이 항상 "꺼짐".
    private void requireValidApiKey(String apiKey) {
        if (!StringUtils.hasText(testApiKey) || !testApiKey.equals(apiKey)) {
            throw new CustomException(GlobalErrorCode.FORBIDDEN);
        }
    }

    @Schema(name = "TeamCreationTestResponse", description = "즉시 생성된 팀(채팅방) 정보")
    public record TeamCreationTestResponse(
            @Schema(description = "생성된 팀(채팅방) ID — 이후 채팅/투표 API의 teamId로 그대로 사용", example = "5") Long teamId,
            @Schema(description = "팀 상태 — 인사 유도 단계로 시작한다", example = "GREETING") String status,
            @Schema(description = "leaderPreference 조합으로 계산된 팀장 선출 모드", example = "AUTO_ASSIGNED")
                    String leaderSelectionMode) {}
}
