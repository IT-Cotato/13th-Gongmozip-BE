package org.cotato.gongmozip.domains.matching.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamCreationRequest;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamMemberInput;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.springframework.stereotype.Service;

/**
 * 수락 API, 수동 패스, 12시 마감이 공통으로 사용하는 최종 Team 확정 규칙이다.
 *
 * <p>별도 트랜잭션을 시작하지 않고 호출한 응답 서비스의 쓰기 트랜잭션에 참여한다. Team/TeamMember
 * 저장이나 채팅 인사가 실패하면 그룹·신청 상태까지 함께 롤백돼야 하기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class MatchingGroupCompletionService {

    private final TeamService teamService;

    public Optional<Team> completeIfReady(
            MatchingGroup group, List<MatchingGroupMember> groupMembers, LocalDateTime now) {
        // 응답 상태를 세기 전에 저장 결과 자체가 3/4인 그룹 계약을 만족하는지 검증한다.
        // 잘못 저장된 그룹으로 Team을 만들어 더 큰 데이터 오염을 만드는 것을 막는 마지막 방어다.
        validateGroupShape(group, groupMembers);

        // PASSED/EXPIRED 사용자는 실제 Team 후보에서 제외한다. 4인 제안이어도 active가 3명이면
        // 아래 조건을 통과할 수 있어 후속 합의인 4인 → 3인 확정을 지원한다.
        List<MatchingGroupMember> activeMembers = groupMembers.stream()
                .filter(MatchingGroupMember::isActiveResponseTarget)
                .toList();

        // 한 명이라도 PENDING이면 아직 의사결정이 끝나지 않았다. Team을 만들지 않고 현재 응답만
        // 저장하게 Optional.empty()를 반환한다.
        if (activeMembers.size() < 3
                || activeMembers.stream()
                        .anyMatch(member -> member.getResponseStatus() == MatchingGroupMemberStatus.PENDING)) {
            return Optional.empty();
        }

        // 현재 active 정의상 PENDING이 없으면 사실상 ACCEPTED만 남는다. 그래도 상태 모델이 확장될
        // 가능성을 고려해 실제 Team 입력은 ACCEPTED를 명시적으로 다시 필터링한다.
        List<MatchingGroupMember> acceptedMembers = activeMembers.stream()
                .filter(member -> member.getResponseStatus() == MatchingGroupMemberStatus.ACCEPTED)
                .toList();
        if (acceptedMembers.size() < 3 || acceptedMembers.size() > 4) {
            return Optional.empty();
        }
        if (group.getTeam() != null) {
            throw new MatchingException(MatchingErrorCode.MATCHING_TEAM_ALREADY_CREATED);
        }

        // 현재 회원/프로필을 새로 선택하지 않고 매칭 신청에 저장된 선택 프로필 ID를 사용한다.
        // Team 카테고리도 재계산하지 않고 결과 그룹의 카테고리를 그대로 전달한다.
        TeamCreationRequest request = new TeamCreationRequest(
                acceptedMembers.stream().map(this::toTeamMemberInput).toList(), group.getCategory());

        // TeamService 안에서 TeamMember 저장과 채팅 인사 시작까지 끝난 뒤에만 매칭 상태를 확정한다.
        // 여기서 예외가 나면 아래 MATCHED/CONFIRMED 전이는 실행되지 않고 바깥 트랜잭션도 롤백된다.
        Team team = teamService.createTeam(request);
        acceptedMembers.forEach(member -> member.getMatchingApplication().match());
        group.confirm(team, acceptedMembers.size(), now);
        return Optional.of(team);
    }

    private void validateGroupShape(MatchingGroup group, List<MatchingGroupMember> groupMembers) {
        if (group.getTeamSize() == null
                || (group.getTeamSize() != 3 && group.getTeamSize() != 4)
                || groupMembers.size() != group.getTeamSize()) {
            throw new IllegalStateException("저장된 매칭 그룹은 정확히 3명 또는 4명으로 구성돼야 합니다.");
        }
        long distinctMemberCount = groupMembers.stream()
                .map(member -> member.getMember().getMemberId())
                .distinct()
                .count();
        if (distinctMemberCount != groupMembers.size()) {
            throw new IllegalStateException("매칭 그룹에 같은 회원이 중복으로 포함될 수 없습니다.");
        }
    }

    private TeamMemberInput toTeamMemberInput(MatchingGroupMember groupMember) {
        MatchingApplication application = groupMember.getMatchingApplication();
        Long memberId = groupMember.getMember().getMemberId();
        // 그룹원, 신청자, 선택 프로필 소유자가 다르면 다른 사람의 프로필로 TeamMember가 생성될 수
        // 있으므로 TeamService 호출 전에 세 ID의 소유 관계를 확인한다.
        if (!memberId.equals(application.getMember().getMemberId())
                || !memberId.equals(application.getProfile().getMember().getMemberId())) {
            throw new IllegalStateException("매칭 신청의 회원과 선택 프로필 소유자가 일치하지 않습니다.");
        }
        return new TeamMemberInput(
                memberId,
                application.getProfile().getProfileId(),
                application.getLeaderPreference(),
                application.getExtroversionType(),
                application.getExtroversionScore());
    }
}
