package org.cotato.gongmozip.domains.team.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamCreationRequest;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ 임시 개발용 엔드포인트. 매칭 도메인이 {@link TeamService#createTeam}을 직접 호출하게
 * 되면 이 컨트롤러는 삭제한다 (docs/decisions/01-team.md의 TeamCreationRequest 계약 참고).
 * {@code local} 프로필을 명시적으로 활성화했을 때만 켜진다(기본값은 비활성).
 */
@Profile("local")
@Tag(name = "TeamTest", description = "[개발용] Team 생성 테스트 엔드포인트 - 매칭 연동 전까지만 사용")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TeamTestController {

    private final TeamService teamService;

    @Operation(summary = "[개발용] 팀 생성 (TeamCreationRequest 그대로 전달)")
    @PostMapping("/teams")
    public TeamCreatedResponse createTeam(@RequestBody TeamCreationRequest request) {
        Team team = teamService.createTeam(request);
        return new TeamCreatedResponse(team.getTeamId());
    }

    public record TeamCreatedResponse(Long teamId) {}
}
