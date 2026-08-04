package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.matching.support.MatchingResponseFixture;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingResponseServiceTest {

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
    private MatchingGroupCompletionService matchingGroupCompletionService;

    @Mock
    private MatchingTimePolicy matchingTimePolicy;

    private MatchingResponseService matchingResponseService;

    @BeforeEach
    void setUp() {
        matchingResponseService = new MatchingResponseService(
                matchingGroupRepository,
                matchingGroupMemberRepository,
                memberRepository,
                matchingPassPenaltyService,
                matchingReassignmentService,
                matchingGroupCompletionService,
                matchingTimePolicy);
    }

    @Test
    void 공개_전에는_수락할_수_없다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(List.of(requester));
        given(matchingTimePolicy.now()).willReturn(MatchingResponseFixture.PUBLISHED_AT.minusSeconds(1));
        given(matchingTimePolicy.isResultPublished(
                        MatchingResponseFixture.APPLICATION_DATE, MatchingResponseFixture.PUBLISHED_AT.minusSeconds(1)))
                .willReturn(false);

        assertThatThrownBy(() -> matchingResponseService.accept(1L, 20L))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_RESULT_NOT_PUBLISHED);
        verify(matchingGroupCompletionService, never()).completeIfReady(any(), any(), any());
    }

    @Test
    void 공개_전에도_패널티를_받고_철회할_수_있다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 4);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        List<MatchingGroupMember> members = List.of(
                requester,
                member(2L, group, 2L, MatchingGroupMemberStatus.PENDING),
                member(3L, group, 3L, MatchingGroupMemberStatus.PENDING),
                member(4L, group, 4L, MatchingGroupMemberStatus.PENDING));
        var beforePublish = MatchingResponseFixture.PUBLISHED_AT.minusSeconds(1);
        given(matchingGroupMemberRepository.findGroupIdByApplicationAndMember(
                        requester.getMatchingApplication().getMatchingApplicationId(), 1L))
                .willReturn(Optional.of(group.getMatchingGroupId()));
        given(matchingGroupRepository.findByIdWithLock(group.getMatchingGroupId()))
                .willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(members);
        given(matchingTimePolicy.now()).willReturn(beforePublish);
        given(memberRepository.findByIdWithLock(1L)).willReturn(Optional.of(requester.getMember()));
        given(matchingPassPenaltyService.apply(requester.getMember(), beforePublish))
                .willReturn(3);

        var response = matchingResponseService.pass(
                1L, requester.getMatchingApplication().getMatchingApplicationId());

        assertThat(response.collaborationPenalty()).isEqualTo(3);
        assertThat(requester.getResponseStatus()).isEqualTo(MatchingGroupMemberStatus.PASSED);
        assertThat(requester.getMatchingApplication().getStatus()).isEqualTo(MatchingApplicationStatus.PASSED);
        verify(matchingTimePolicy, never()).isResultPublished(any(), any());
    }

    @Test
    void 마감_정각부터는_수락할_수_없다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(List.of(requester));
        given(matchingTimePolicy.now()).willReturn(MatchingResponseFixture.DEADLINE_AT);
        stubPublished(MatchingResponseFixture.DEADLINE_AT);

        assertThatThrownBy(() -> matchingResponseService.accept(1L, 20L))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_RESPONSE_DEADLINE_PASSED);
        verify(matchingGroupCompletionService, never()).completeIfReady(any(), any(), any());
    }

    @Test
    void 네명중_한명이_패스해도_세명이_남으면_그룹을_유지한다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 4);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        List<MatchingGroupMember> members = List.of(
                requester,
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.PENDING),
                member(4L, group, 4L, MatchingGroupMemberStatus.PENDING));
        stubPass(group, requester, members);

        var response = matchingResponseService.pass(
                1L, requester.getMatchingApplication().getMatchingApplicationId());

        assertThat(response.collaborationPenalty()).isEqualTo(3);
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.PROPOSED);
        assertThat(requester.getResponseStatus()).isEqualTo(MatchingGroupMemberStatus.PASSED);
        verify(matchingGroupCompletionService).completeIfReady(group, members, now());
        verify(matchingReassignmentService, never()).register(any(), any());
    }

    @Test
    void 세명중_한명이_패스하면_그룹을_취소하고_나머지_두명만_재매칭한다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        List<MatchingGroupMember> members = List.of(
                requester,
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.PENDING));
        stubPass(group, requester, members);

        matchingResponseService.pass(1L, requester.getMatchingApplication().getMatchingApplicationId());

        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.CANCELED);
        verify(matchingReassignmentService, times(2))
                .register(any(), org.mockito.ArgumentMatchers.eq(MatchingReassignmentReason.GROUP_MEMBER_PASSED));
        verify(matchingGroupCompletionService, never()).completeIfReady(any(), any(), any());
    }

    @Test
    void 같은_패스_요청을_반복해도_감점을_다시_적용하지_않는다() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        requester.pass(now(), 3);
        requester.getMatchingApplication().pass(now());
        given(matchingGroupMemberRepository.findGroupIdByApplicationAndMember(
                        requester.getMatchingApplication().getMatchingApplicationId(), 1L))
                .willReturn(Optional.of(20L));
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(List.of(requester));
        given(matchingTimePolicy.now()).willReturn(now());

        var response = matchingResponseService.pass(
                1L, requester.getMatchingApplication().getMatchingApplicationId());

        assertThat(response.collaborationPenalty()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(MatchingApplicationStatus.PASSED.name());
        verify(matchingPassPenaltyService, never()).apply(any(), any());
    }

    @Test
    void passIsRejectedAtResponseDeadline() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        Long applicationId = requester.getMatchingApplication().getMatchingApplicationId();
        given(matchingGroupMemberRepository.findGroupIdByApplicationAndMember(applicationId, 1L))
                .willReturn(Optional.of(20L));
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(List.of(requester));
        given(matchingTimePolicy.now()).willReturn(MatchingResponseFixture.DEADLINE_AT);

        assertThatThrownBy(() -> matchingResponseService.pass(1L, applicationId))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_RESPONSE_DEADLINE_PASSED);
        verify(matchingGroupCompletionService, never()).completeIfReady(any(), any(), any());
    }

    @Test
    void repeatedAcceptReturnsExistingResultWithoutCompletingAgain() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.ACCEPTED);
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(List.of(requester));
        given(matchingTimePolicy.now()).willReturn(now());
        stubPublished(now());

        var response = matchingResponseService.accept(1L, 20L);

        assertThat(response.myResponseStatus()).isEqualTo(MatchingGroupMemberStatus.ACCEPTED);
        verify(matchingGroupCompletionService, never()).completeIfReady(any(), any(), any());
    }

    @Test
    void lastAcceptConfirmsTeamOnlyOnceAcrossRepeatedRequest() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.PENDING);
        List<MatchingGroupMember> members = List.of(
                requester,
                member(2L, group, 2L, MatchingGroupMemberStatus.ACCEPTED),
                member(3L, group, 3L, MatchingGroupMemberStatus.ACCEPTED));
        Team team = Team.builder().teamId(99L).build();
        given(matchingGroupRepository.findByIdWithLock(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(members);
        given(matchingTimePolicy.now()).willReturn(now());
        stubPublished(now());
        given(matchingGroupCompletionService.completeIfReady(group, members, now()))
                .willAnswer(invocation -> {
                    members.forEach(member -> member.getMatchingApplication().match());
                    group.confirm(team, 3, now());
                    return Optional.of(team);
                });

        var first = matchingResponseService.accept(1L, 20L);
        var repeated = matchingResponseService.accept(1L, 20L);

        assertThat(first.teamId()).isEqualTo(99L);
        assertThat(repeated.teamId()).isEqualTo(99L);
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.CONFIRMED);
        verify(matchingGroupCompletionService, times(1)).completeIfReady(group, members, now());
    }

    private void stubPass(MatchingGroup group, MatchingGroupMember requester, List<MatchingGroupMember> members) {
        given(matchingGroupMemberRepository.findGroupIdByApplicationAndMember(
                        requester.getMatchingApplication().getMatchingApplicationId(), 1L))
                .willReturn(Optional.of(group.getMatchingGroupId()));
        given(matchingGroupRepository.findByIdWithLock(group.getMatchingGroupId()))
                .willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findAllByMatchingGroupWithLock(group))
                .willReturn(members);
        given(matchingTimePolicy.now()).willReturn(now());
        given(memberRepository.findByIdWithLock(1L)).willReturn(Optional.of(requester.getMember()));
        given(matchingPassPenaltyService.apply(requester.getMember(), now())).willReturn(3);
    }

    private void stubPublished(java.time.LocalDateTime currentTime) {
        given(matchingTimePolicy.isResultPublished(MatchingResponseFixture.APPLICATION_DATE, currentTime))
                .willReturn(true);
    }

    private MatchingGroupMember member(
            Long groupMemberId, MatchingGroup group, Long memberId, MatchingGroupMemberStatus status) {
        return MatchingResponseFixture.groupMember(groupMemberId, group, memberId, status);
    }

    private java.time.LocalDateTime now() {
        return MatchingResponseFixture.PUBLISHED_AT.plusMinutes(30);
    }
}
