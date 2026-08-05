package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingReassignmentReason;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.matching.support.MatchingResponseFixture;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingResponseDeadlineServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingGroupMemberRepository matchingGroupMemberRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MatchingPassPenaltyService matchingPassPenaltyService;

    @Mock
    private MatchingReassignmentService matchingReassignmentService;

    @Mock
    private TeamService teamService;

    @Mock
    private MatchingTimePolicy matchingTimePolicy;

    private MatchingResponseDeadlineService deadlineService;

    @BeforeEach
    void setUp() {
        deadlineService = new MatchingResponseDeadlineService(
                matchingGroupRepository,
                matchingGroupMemberRepository,
                memberRepository,
                matchingPassPenaltyService,
                matchingReassignmentService,
                new MatchingGroupCompletionService(teamService),
                matchingTimePolicy);
    }

    @Test
    void 네명중_세명이_수락했다면_마감에_미응답자를_만료시키고_세명으로_팀을_만든다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 4);
        List<MatchingGroupMember> members = List.of(
                member(1L, group, 1L, MatchingGroupMemberStatus.ACCEPTED),
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.ACCEPTED),
                member(4L, group, 4L, MatchingGroupMemberStatus.PENDING));
        stubDeadline(group, members, members.get(3));
        Team team = Team.builder().teamId(99L).build();
        given(teamService.createTeam(any())).willReturn(team);

        deadlineService.processGroup(20L);

        assertThat(members.get(3).getResponseStatus()).isEqualTo(MatchingGroupMemberStatus.EXPIRED);
        assertThat(members.get(3).getMatchingApplication().getStatus()).isEqualTo(MatchingApplicationStatus.PASSED);
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.CONFIRMED);
        assertThat(group.getConfirmedTeamSize()).isEqualTo(3);
        verify(teamService).createTeam(any());
        verify(matchingReassignmentService, times(0)).register(any(), any());
    }

    @Test
    void 세명중_두명만_수락했다면_마감에_그룹을_만료하고_수락자만_재매칭한다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        List<MatchingGroupMember> members = List.of(
                member(1L, group, 1L, MatchingGroupMemberStatus.ACCEPTED),
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.PENDING));
        stubDeadline(group, members, members.get(2));

        deadlineService.processGroup(20L);

        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.EXPIRED);
        verify(matchingReassignmentService, times(2))
                .register(any(), org.mockito.ArgumentMatchers.eq(MatchingReassignmentReason.RESPONSE_DEADLINE_EXPIRED));
        verify(teamService, times(0)).createTeam(any());
    }

    @Test
    void 이미_종료된_그룹은_재실행해도_아무것도_처리하지_않는다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        group.expire(MatchingResponseFixture.DEADLINE_AT);
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));

        deadlineService.processGroup(20L);

        verify(matchingGroupMemberRepository, times(0)).findAllByMatchingGroupWithLock(any());
        verify(matchingPassPenaltyService, times(0)).apply(any(), any());
    }

    private void stubDeadline(
            MatchingGroup group, List<MatchingGroupMember> members, MatchingGroupMember pendingMember) {
        given(matchingGroupRepository.findByIdWithLock(group.getMatchingGroupId()))
                .willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(members);
        given(matchingTimePolicy.now()).willReturn(MatchingResponseFixture.DEADLINE_AT);
        given(memberRepository.findByIdWithLock(pendingMember.getMember().getMemberId()))
                .willReturn(Optional.of(pendingMember.getMember()));
        given(matchingPassPenaltyService.apply(pendingMember.getMember(), MatchingResponseFixture.DEADLINE_AT))
                .willReturn(3);
    }

    private MatchingGroupMember member(
            Long groupMemberId, MatchingGroup group, Long memberId, MatchingGroupMemberStatus status) {
        return MatchingResponseFixture.groupMember(groupMemberId, group, memberId, status);
    }
}
