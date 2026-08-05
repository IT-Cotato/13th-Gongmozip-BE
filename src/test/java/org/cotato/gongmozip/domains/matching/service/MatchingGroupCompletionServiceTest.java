package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.support.MatchingResponseFixture;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.team.dto.request.TeamRequest.TeamCreationRequest;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingGroupCompletionServiceTest {

    @Mock
    private TeamService teamService;

    @Test
    void 네명중_한명이_패스하고_세명이_수락하면_세명으로_팀을_생성한다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 4);
        List<MatchingGroupMember> members = List.of(
                member(1L, group, 1L, MatchingGroupMemberStatus.ACCEPTED),
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.ACCEPTED),
                member(4L, group, 4L, MatchingGroupMemberStatus.PASSED));
        Team team = Team.builder().teamId(99L).build();
        given(teamService.createTeam(any())).willReturn(team);
        MatchingGroupCompletionService service = new MatchingGroupCompletionService(teamService);

        var result = service.completeIfReady(group, members, MatchingResponseFixture.PUBLISHED_AT.plusMinutes(10));

        assertThat(result).contains(team);
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.CONFIRMED);
        assertThat(group.getConfirmedTeamSize()).isEqualTo(3);
        assertThat(members.subList(0, 3))
                .allSatisfy(member -> assertThat(member.getMatchingApplication().getStatus())
                        .isEqualTo(MatchingApplicationStatus.MATCHED));
        assertThat(members.get(3).getMatchingApplication().getStatus()).isEqualTo(MatchingApplicationStatus.PASSED);
        ArgumentCaptor<TeamCreationRequest> requestCaptor = ArgumentCaptor.forClass(TeamCreationRequest.class);
        verify(teamService).createTeam(requestCaptor.capture());
        assertThat(requestCaptor.getValue().members()).hasSize(3);
        assertThat(requestCaptor.getValue().members()).allSatisfy(input -> {
            assertThat(input.leaderPreference()).isEqualTo(LeaderPreference.NEUTRAL);
            assertThat(input.extroversionType()).isEqualTo(ExtroversionType.A);
            assertThat(input.extroversionScore()).isEqualByComparingTo(new BigDecimal("50.00"));
        });
    }

    @Test
    void 팀_생성이_실패하면_그룹과_신청을_확정하지_않는다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        List<MatchingGroupMember> members = List.of(
                member(1L, group, 1L, MatchingGroupMemberStatus.ACCEPTED),
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.ACCEPTED));
        RuntimeException failure = new RuntimeException("team creation failed");
        given(teamService.createTeam(any())).willThrow(failure);
        MatchingGroupCompletionService service = new MatchingGroupCompletionService(teamService);

        assertThatThrownBy(() -> service.completeIfReady(group, members, MatchingResponseFixture.PUBLISHED_AT))
                .isSameAs(failure);

        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.PROPOSED);
        assertThat(members)
                .allSatisfy(member -> assertThat(member.getMatchingApplication().getStatus())
                        .isEqualTo(MatchingApplicationStatus.PROPOSED));
    }

    private MatchingGroupMember member(
            Long groupMemberId, MatchingGroup group, Long memberId, MatchingGroupMemberStatus responseStatus) {
        MatchingGroupMember member =
                MatchingResponseFixture.groupMember(groupMemberId, group, memberId, responseStatus);
        if (responseStatus == MatchingGroupMemberStatus.PASSED) {
            member.getMatchingApplication().pass(MatchingResponseFixture.PUBLISHED_AT);
        }
        return member;
    }
}
