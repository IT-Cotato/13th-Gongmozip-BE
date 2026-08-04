package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingReassignmentReason;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.support.MatchingResponseFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingReassignmentServiceTest {

    @Mock
    private MatchingApplicationRepository matchingApplicationRepository;

    @Mock
    private MatchingTimePolicy matchingTimePolicy;

    private MatchingReassignmentService matchingReassignmentService;

    @BeforeEach
    void setUp() {
        matchingReassignmentService =
                new MatchingReassignmentService(matchingApplicationRepository, matchingTimePolicy);
    }

    @Test
    void reassignmentCopiesOriginalSnapshotAndRaisesPriority() {
        MatchingApplication source = sourceApplication();
        given(matchingTimePolicy.nextAvailableMatchingDate(MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(MatchingResponseFixture.APPLICATION_DATE.plusDays(1));
        given(matchingApplicationRepository.findByMemberAndApplicationDate(
                        source.getMember(), MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(Optional.empty());
        given(matchingApplicationRepository.save(any(MatchingApplication.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MatchingApplication reassignment =
                matchingReassignmentService.register(source, MatchingReassignmentReason.GROUP_MEMBER_PASSED);

        assertThat(source.getStatus()).isEqualTo(MatchingApplicationStatus.REASSIGN_PENDING);
        assertThat(reassignment.getStatus()).isEqualTo(MatchingApplicationStatus.WAITING);
        assertThat(reassignment.getApplicationDate()).isEqualTo(MatchingResponseFixture.APPLICATION_DATE.plusDays(1));
        assertThat(reassignment.getSourceApplication()).isSameAs(source);
        assertThat(reassignment.getReassignmentReason()).isEqualTo(MatchingReassignmentReason.GROUP_MEMBER_PASSED);
        assertThat(reassignment.getReassignmentCount()).isEqualTo(1);
        assertThat(reassignment.isReassignmentPriority()).isTrue();
        assertThat(reassignment)
                .usingRecursiveComparison()
                .ignoringFields(
                        "matchingApplicationId",
                        "applicationDate",
                        "status",
                        "matchingBatch",
                        "skillGroup",
                        "sourceApplication",
                        "reassignmentCount",
                        "reassignmentReason",
                        "reassignmentPriority",
                        "canceledAt",
                        "createdAt",
                        "updatedAt")
                .isEqualTo(source);
    }

    @Test
    void directApplicationOnReassignmentDateCausesConflict() {
        MatchingApplication source = sourceApplication();
        given(matchingTimePolicy.nextAvailableMatchingDate(MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(MatchingResponseFixture.APPLICATION_DATE.plusDays(1));
        MatchingApplication directApplication = MatchingResponseFixture.groupMember(
                        2L,
                        MatchingResponseFixture.group(21L, 3),
                        source.getMember().getMemberId(),
                        MatchingGroupMemberStatus.PENDING)
                .getMatchingApplication();
        given(matchingApplicationRepository.findByMemberAndApplicationDate(
                        source.getMember(), MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(Optional.of(directApplication));

        assertThatThrownBy(() -> matchingReassignmentService.register(
                        source, MatchingReassignmentReason.RESPONSE_DEADLINE_EXPIRED))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_REASSIGNMENT_CONFLICT);
        verify(matchingApplicationRepository, never()).save(any());
    }

    @Test
    void repeatedRegistrationReturnsExistingAutomaticReassignment() {
        MatchingApplication source = sourceApplication();
        given(matchingTimePolicy.nextAvailableMatchingDate(MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(MatchingResponseFixture.APPLICATION_DATE.plusDays(1));
        MatchingApplication existing = source.createReassignment(
                MatchingResponseFixture.APPLICATION_DATE.plusDays(1), MatchingReassignmentReason.GROUP_MEMBER_PASSED);
        given(matchingApplicationRepository.findByMemberAndApplicationDate(
                        source.getMember(), MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(Optional.of(existing));

        assertThat(matchingReassignmentService.register(source, MatchingReassignmentReason.GROUP_MEMBER_PASSED))
                .isSameAs(existing);
        verify(matchingApplicationRepository, never()).save(any());
    }

    @Test
    void delayedReassignmentUsesNextUnstartedMatchingDate() {
        MatchingApplication source = sourceApplication();
        var nextUnstartedDate = MatchingResponseFixture.APPLICATION_DATE.plusDays(2);
        given(matchingTimePolicy.nextAvailableMatchingDate(MatchingResponseFixture.APPLICATION_DATE.plusDays(1)))
                .willReturn(nextUnstartedDate);
        given(matchingApplicationRepository.findByMemberAndApplicationDate(source.getMember(), nextUnstartedDate))
                .willReturn(Optional.empty());
        given(matchingApplicationRepository.save(any(MatchingApplication.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        MatchingApplication reassignment =
                matchingReassignmentService.register(source, MatchingReassignmentReason.RESPONSE_DEADLINE_EXPIRED);

        assertThat(reassignment.getApplicationDate()).isEqualTo(nextUnstartedDate);
        assertThat(reassignment.getStatus()).isEqualTo(MatchingApplicationStatus.WAITING);
    }

    private MatchingApplication sourceApplication() {
        return MatchingResponseFixture.groupMember(
                        1L, MatchingResponseFixture.group(20L, 3), 1L, MatchingGroupMemberStatus.ACCEPTED)
                .getMatchingApplication();
    }
}
