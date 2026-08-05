package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.matching.support.MatchingResponseFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingGroupQueryServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingGroupMemberRepository matchingGroupMemberRepository;

    @Mock
    private MatchingTimePolicy matchingTimePolicy;

    private MatchingGroupQueryService matchingGroupQueryService;

    @BeforeEach
    void setUp() {
        matchingGroupQueryService = new MatchingGroupQueryService(
                matchingGroupRepository, matchingGroupMemberRepository, matchingTimePolicy);
    }

    @Test
    void memberCanReadResponsesOfOwnGroup() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        MatchingGroupMember requester = member(1L, group, 1L, MatchingGroupMemberStatus.ACCEPTED);
        MatchingGroupMember pending = member(2L, group, 2L, MatchingGroupMemberStatus.PENDING);
        MatchingGroupMember passed = member(3L, group, 3L, MatchingGroupMemberStatus.PASSED);
        given(matchingGroupRepository.findById(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findByMatchingGroup_MatchingGroupIdAndMember_MemberId(20L, 1L))
                .willReturn(Optional.of(requester));
        given(matchingGroupMemberRepository.findResultMembers(group)).willReturn(List.of(requester, pending, passed));
        given(matchingTimePolicy.now()).willReturn(MatchingResponseFixture.PUBLISHED_AT.plusMinutes(1));
        given(matchingTimePolicy.resultPublishAt(MatchingResponseFixture.APPLICATION_DATE))
                .willReturn(MatchingResponseFixture.PUBLISHED_AT);
        given(matchingTimePolicy.isResultPublished(
                        MatchingResponseFixture.APPLICATION_DATE, MatchingResponseFixture.PUBLISHED_AT.plusMinutes(1)))
                .willReturn(true);

        var response = matchingGroupQueryService.getResponses(1L, 20L);

        assertThat(response.groupStatus()).isEqualTo(MatchingGroupStatus.PROPOSED);
        assertThat(response.responseDeadlineAt()).isEqualTo(MatchingResponseFixture.DEADLINE_AT);
        assertThat(response.activeMemberCount()).isEqualTo(2);
        assertThat(response.members())
                .extracting(member -> member.responseStatus())
                .containsExactly(
                        MatchingGroupMemberStatus.ACCEPTED,
                        MatchingGroupMemberStatus.PENDING,
                        MatchingGroupMemberStatus.PASSED);
        assertThat(response.members())
                .filteredOn(member -> member.me())
                .singleElement()
                .extracting(member -> member.memberId())
                .isEqualTo(1L);
    }

    @Test
    void memberCannotReadResponsesOfAnotherGroup() {
        MatchingGroup group = MatchingResponseFixture.group(20L, 3);
        given(matchingGroupRepository.findById(20L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.findByMatchingGroup_MatchingGroupIdAndMember_MemberId(20L, 99L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> matchingGroupQueryService.getResponses(99L, 20L))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED);
    }

    private MatchingGroupMember member(
            Long groupMemberId, MatchingGroup group, Long memberId, MatchingGroupMemberStatus status) {
        return MatchingResponseFixture.groupMember(groupMemberId, group, memberId, status);
    }
}
