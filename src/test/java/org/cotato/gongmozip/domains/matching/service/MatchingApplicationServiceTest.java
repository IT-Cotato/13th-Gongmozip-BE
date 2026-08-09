package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.repository.CollaborationPointHistoryRepository;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.matching.dto.request.MatchingApplicationRequest.ApplyRequest;
import org.cotato.gongmozip.domains.matching.entity.MatchingApplication;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroupMember;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupMemberStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingIneligibilityReason;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.score.ProjectScoreProvider;
import org.cotato.gongmozip.domains.matching.score.SkillScoreCalculator;
import org.cotato.gongmozip.domains.matching.support.MatchingResponseFixture;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.AwardRepository;
import org.cotato.gongmozip.domains.profile.repository.ProfileCertificationRepository;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchingApplicationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 31);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 31, 13, 0);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProjectExperienceRepository projectExperienceRepository;

    @Mock
    private AwardRepository awardRepository;

    @Mock
    private ProfileCertificationRepository profileCertificationRepository;

    @Mock
    private SurveySubmissionRepository surveySubmissionRepository;

    @Mock
    private MatchingApplicationRepository matchingApplicationRepository;

    @Mock
    private MatchingGroupMemberRepository matchingGroupMemberRepository;

    @Mock
    private CollaborationPointHistoryRepository collaborationPointHistoryRepository;

    @Mock
    private CollaborationPointService collaborationPointService;

    @Mock
    private ProjectScoreProvider projectScoreProvider;

    @Mock
    private MatchingTimePolicy matchingTimePolicy;

    private MatchingApplicationService matchingApplicationService;

    @BeforeEach
    void setUp() {
        matchingApplicationService = new MatchingApplicationService(
                memberRepository,
                profileRepository,
                projectExperienceRepository,
                awardRepository,
                profileCertificationRepository,
                surveySubmissionRepository,
                matchingApplicationRepository,
                matchingGroupMemberRepository,
                collaborationPointHistoryRepository,
                new MatchingPassPenaltyService(matchingApplicationRepository, collaborationPointService),
                projectScoreProvider,
                new SkillScoreCalculator(),
                matchingTimePolicy);
    }

    @DisplayName("신청할 수 없는 모든 사유를 누락 없이 함께 반환한다.")
    @Test
    void getEligibilityReturnsAllIneligibilityReasons() {
        LocalDateTime blockedUntil = NOW.plusDays(1);
        Member member =
                Member.builder().memberId(1L).matchingBlockedUntil(blockedUntil).build();
        given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(profileRepository.countByMember(member)).willReturn(0);
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.empty());
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TODAY))
                .willReturn(true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(false);
        given(matchingApplicationRepository.countByApplicationDateAndStatusIn(any(LocalDate.class), any()))
                .willReturn(12L);
        given(matchingTimePolicy.applicationDeadline(TODAY)).willReturn(TODAY.atTime(14, 0));

        var response = matchingApplicationService.getEligibility(member.getMemberId());

        assertThat(response.eligible()).isFalse();
        assertThat(response.reasons())
                .containsExactly(
                        MatchingIneligibilityReason.PROFILE_REQUIRED,
                        MatchingIneligibilityReason.SURVEY_REQUIRED,
                        MatchingIneligibilityReason.APPLICATION_DEADLINE_PASSED,
                        MatchingIneligibilityReason.ALREADY_APPLIED_TODAY,
                        MatchingIneligibilityReason.MATCHING_RESTRICTED);
        assertThat(response.hasProfile()).isFalse();
        assertThat(response.surveyCompleted()).isFalse();
        assertThat(response.appliedToday()).isTrue();
        assertThat(response.matchingBlockedUntil()).isEqualTo(blockedUntil);
        assertThat(response.applicationDate()).isEqualTo(TODAY);
        assertThat(response.participantCount()).isEqualTo(12);
    }

    @DisplayName("오늘 신청이 없으면 NONE 상태의 빈 응답을 반환한다.")
    @Test
    void getTodayApplicationReturnsEmptyResponse() {
        Member member = Member.builder().memberId(1L).build();
        given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingTimePolicy.today()).willReturn(TODAY);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingApplicationRepository.findByMemberAndApplicationDate(member, TODAY))
                .willReturn(Optional.empty());

        var response = matchingApplicationService.getTodayApplication(member.getMemberId());

        assertThat(response.appliedToday()).isFalse();
        assertThat(response.applicationId()).isNull();
        assertThat(response.status()).isEqualTo("NONE");
        assertThat(response.withdrawal()).isNull();
    }

    @DisplayName("철회할 수 없는 신청 상태는 철회 불가 정보로 반환한다.")
    @Test
    void getTodayApplicationReturnsUnavailableWithdrawalForNonWithdrawableStatus() {
        Member member = Member.builder().memberId(1L).build();
        MatchingApplication application = applicationWithStatus(100L, member, MatchingApplicationStatus.MATCHED);
        given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingTimePolicy.today()).willReturn(TODAY);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingApplicationRepository.findByMemberAndApplicationDate(member, TODAY))
                .willReturn(Optional.of(application));

        var response = matchingApplicationService.getTodayApplication(member.getMemberId());

        assertThat(response.appliedToday()).isTrue();
        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.withdrawal().withdrawable()).isFalse();
        assertThat(response.withdrawal().type()).isNull();
        assertThat(response.withdrawal().expectedPenalty()).isZero();
        assertThat(response.withdrawal().deadlineAt()).isNull();
        verify(matchingTimePolicy, never()).resolveWithdrawalType(any(LocalDate.class));
    }

    @DisplayName("결과 공개 이후 익일 신청이 없으면 오늘자 신청으로 폴백해 조회한다.")
    @Test
    void getTodayApplicationFallsBackToTodayAfterResultPublish() {
        Member member = Member.builder().memberId(1L).build();
        MatchingApplication application = applicationWithStatus(100L, member, MatchingApplicationStatus.MATCHED);
        given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingTimePolicy.today()).willReturn(TODAY);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TOMORROW);
        given(matchingApplicationRepository.findByMemberAndApplicationDate(member, TOMORROW))
                .willReturn(Optional.empty());
        given(matchingApplicationRepository.findByMemberAndApplicationDate(member, TODAY))
                .willReturn(Optional.of(application));

        var response = matchingApplicationService.getTodayApplication(member.getMemberId());

        assertThat(response.appliedToday()).isTrue();
        assertThat(response.applicationDate()).isEqualTo(TODAY);
        assertThat(response.status()).isEqualTo("MATCHED");
    }

    @DisplayName("결과 공개 전에도 제안된 신청은 패널티 철회 가능으로 반환한다.")
    @Test
    void proposedApplicationIsWithdrawableBeforeResultPublication() {
        MatchingGroupMember membership = MatchingResponseFixture.groupMember(
                1L, MatchingResponseFixture.group(20L, 4), 1L, MatchingGroupMemberStatus.PENDING);
        Member member = membership.getMember();
        MatchingApplication application = membership.getMatchingApplication();
        LocalDateTime beforePublish = MatchingResponseFixture.PUBLISHED_AT.minusSeconds(1);
        given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingTimePolicy.today()).willReturn(TODAY);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(beforePublish);
        given(matchingApplicationRepository.findByMemberAndApplicationDate(member, TODAY))
                .willReturn(Optional.of(application));
        given(matchingGroupMemberRepository.findResultMembership(application)).willReturn(Optional.of(membership));

        var response = matchingApplicationService.getTodayApplication(member.getMemberId());

        assertThat(response.withdrawal().withdrawable()).isTrue();
        assertThat(response.withdrawal().type()).isEqualTo(WithdrawalType.PENALIZED_PASS);
        assertThat(response.withdrawal().deadlineAt())
                .isEqualTo(membership.getMatchingGroup().getResponseDeadlineAt());
        verify(matchingTimePolicy, never()).isResultPublished(any(), any());
    }

    @DisplayName("신청할 때 역량과 설문 및 협업거리를 신청 엔티티에 스냅샷으로 저장한다.")
    @Test
    void applyStoresSnapshot() {
        Member member = Member.builder().memberId(1L).collaborationPoint(100).build();
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .gpa(4.0)
                .gpaScale(4.0)
                .build();
        SurveySubmission survey = submittedSurvey(member);
        ApplyRequest request =
                new ApplyRequest(profile.getProfileId(), InterestCategory.IT_AI_TECH, LeaderPreference.WANTS, true);

        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(matchingTimePolicy.applicationDeadline(TODAY)).willReturn(TODAY.atTime(14, 0));
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TODAY))
                .willReturn(false);
        given(profileRepository.countByMember(member)).willReturn(1);
        given(profileRepository.findByProfileIdAndMember(profile.getProfileId(), member))
                .willReturn(Optional.of(profile));
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(survey));
        given(projectExperienceRepository.findAllByProfile(profile)).willReturn(List.of());
        given(projectScoreProvider.evaluate(List.of())).willReturn(new BigDecimal("50"));
        given(collaborationPointHistoryRepository.existsByMember(member)).willReturn(false);
        given(awardRepository.countByProfile(profile)).willReturn(1);
        given(profileCertificationRepository.countByProfile(profile)).willReturn(0);
        given(matchingApplicationRepository.save(any(MatchingApplication.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = matchingApplicationService.apply(member.getMemberId(), request);

        ArgumentCaptor<MatchingApplication> captor = ArgumentCaptor.forClass(MatchingApplication.class);
        verify(matchingApplicationRepository).save(captor.capture());
        MatchingApplication saved = captor.getValue();
        assertThat(saved.getApplicationDate()).isEqualTo(TODAY);
        assertThat(saved.getStatus()).isEqualTo(MatchingApplicationStatus.WAITING);
        assertThat(saved.getLeaderPreference()).isEqualTo(LeaderPreference.WANTS);
        assertThat(saved.isFirstMatching()).isTrue();
        assertThat(saved.getCollaborationDistance()).isEqualTo(100);
        assertThat(saved.getCollaborationScore()).isEqualByComparingTo("20.00");
        assertThat(saved.getAgreeablenessScore()).isEqualByComparingTo("4.10");
        assertThat(response.skillScore()).isEqualByComparingTo("52.00");
        assertThat(response.skillGroup()).isNull();
    }

    @DisplayName("결과 공개 이후 신청은 다음 날 매칭으로 저장한다.")
    @Test
    void applyAfterResultPublishStoresNextDayApplication() {
        Member member = Member.builder().memberId(1L).collaborationPoint(100).build();
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .gpa(4.0)
                .gpaScale(4.0)
                .build();
        SurveySubmission survey = submittedSurvey(member);
        ApplyRequest request =
                new ApplyRequest(profile.getProfileId(), InterestCategory.IT_AI_TECH, LeaderPreference.WANTS, true);

        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TOMORROW);
        given(matchingTimePolicy.now()).willReturn(NOW.withHour(17));
        given(matchingTimePolicy.applicationDeadline(TOMORROW)).willReturn(TOMORROW.atTime(14, 0));
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TOMORROW))
                .willReturn(false);
        given(profileRepository.countByMember(member)).willReturn(1);
        given(profileRepository.findByProfileIdAndMember(profile.getProfileId(), member))
                .willReturn(Optional.of(profile));
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(survey));
        given(projectExperienceRepository.findAllByProfile(profile)).willReturn(List.of());
        given(projectScoreProvider.evaluate(List.of())).willReturn(new BigDecimal("50"));
        given(collaborationPointHistoryRepository.existsByMember(member)).willReturn(false);
        given(awardRepository.countByProfile(profile)).willReturn(1);
        given(profileCertificationRepository.countByProfile(profile)).willReturn(0);
        given(matchingApplicationRepository.save(any(MatchingApplication.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = matchingApplicationService.apply(member.getMemberId(), request);

        ArgumentCaptor<MatchingApplication> captor = ArgumentCaptor.forClass(MatchingApplication.class);
        verify(matchingApplicationRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationDate()).isEqualTo(TOMORROW);
        assertThat(response.applicationDate()).isEqualTo(TOMORROW);
        assertThat(response.applicationDeadlineAt()).isEqualTo(TOMORROW.atTime(14, 0));
    }

    @DisplayName("같은 날 취소 이력이 있어도 다시 신청할 수 없다.")
    @Test
    void duplicateApplicationIsRejected() {
        Member member = Member.builder().memberId(1L).build();
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TODAY))
                .willReturn(true);

        assertThatThrownBy(() -> matchingApplicationService.apply(member.getMemberId(), request))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.ALREADY_APPLIED_TODAY);
    }

    @DisplayName("이전 매칭 응답이 열려 있으면 다음 날 직접 신청보다 자동 재매칭을 우선한다.")
    @Test
    void openPreviousResponseBlocksDirectApplication() {
        Member member = Member.builder().memberId(1L).build();
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingGroupMemberRepository.existsOpenResponseForMember(any(), any(), any()))
                .willReturn(true);

        assertThatThrownBy(() -> matchingApplicationService.apply(member.getMemberId(), request))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_REASSIGNMENT_CONFLICT);
        verify(matchingApplicationRepository, never()).existsByMemberAndApplicationDate(any(), any());
    }

    @DisplayName("작성한 프로필이 없으면 매칭을 신청할 수 없다.")
    @Test
    void applicationWithoutProfileIsRejected() {
        Member member = Member.builder().memberId(1L).build();
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TODAY))
                .willReturn(false);
        given(profileRepository.countByMember(member)).willReturn(0);

        assertThatThrownBy(() -> matchingApplicationService.apply(member.getMemberId(), request))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.PROFILE_REQUIRED);
    }

    @DisplayName("매칭 참여 제한 기간에는 신청할 수 없다.")
    @Test
    void restrictedMemberCannotApply() {
        Member member = Member.builder()
                .memberId(1L)
                .matchingBlockedUntil(NOW.plusHours(1))
                .build();
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingTimePolicy.currentApplicationDate()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);

        assertThatThrownBy(() -> matchingApplicationService.apply(member.getMemberId(), request))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_RESTRICTED);
        verify(matchingApplicationRepository, never()).existsByMemberAndApplicationDate(any(), any());
    }

    @DisplayName("14시 전 철회는 무료 취소로 처리한다.")
    @Test
    void withdrawBeforeDeadlineIsFreeCancel() {
        Member member = Member.builder().memberId(1L).collaborationPoint(100).build();
        MatchingApplication application = waitingApplication(100L, member);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.findByIdAndMemberIdWithLock(100L, member.getMemberId()))
                .willReturn(Optional.of(application));
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(matchingTimePolicy.resolveWithdrawalType(TODAY)).willReturn(WithdrawalType.FREE_CANCEL);

        var response = matchingApplicationService.withdraw(member.getMemberId(), 100L);

        assertThat(application.getStatus()).isEqualTo(MatchingApplicationStatus.CANCELED);
        assertThat(response.collaborationPenalty()).isZero();
        verify(collaborationPointService, never())
                .changePoint(any(), any(), any(CollaborationPointReason.class), anyInt());
    }

    @DisplayName("14시 이후 철회는 최근 7일 패스 횟수에 따른 패널티 패스로 처리한다.")
    @Test
    void withdrawAfterDeadlineAppliesEscalatingPassPenalty() {
        Member member = Member.builder().memberId(1L).collaborationPoint(100).build();
        MatchingApplication application = waitingApplication(100L, member);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.findByIdAndMemberIdWithLock(100L, member.getMemberId()))
                .willReturn(Optional.of(application));
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(matchingTimePolicy.resolveWithdrawalType(TODAY)).willReturn(WithdrawalType.PENALIZED_PASS);
        given(matchingApplicationRepository.countByMemberAndStatusAndCanceledAtGreaterThanEqual(
                        member, MatchingApplicationStatus.PASSED, NOW.minusDays(7)))
                .willReturn(2L);

        var response = matchingApplicationService.withdraw(member.getMemberId(), 100L);

        assertThat(application.getStatus()).isEqualTo(MatchingApplicationStatus.PASSED);
        assertThat(response.collaborationPenalty()).isEqualTo(7);
        verify(collaborationPointService).changePoint(member, null, CollaborationPointReason.MATCHING_PASS_PENALTY, -7);
    }

    @DisplayName("타인의 신청은 잠그지 않고 찾을 수 없는 신청으로 처리한다.")
    @Test
    void withdrawDoesNotLockAnotherMembersApplication() {
        Member member = Member.builder().memberId(1L).build();
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.findByIdAndMemberIdWithLock(100L, member.getMemberId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> matchingApplicationService.withdraw(member.getMemberId(), 100L))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.APPLICATION_NOT_FOUND);
    }

    @DisplayName("철회할 수 없는 신청 상태는 거절한다.")
    @Test
    void withdrawRejectsInvalidApplicationStatus() {
        Member member = Member.builder().memberId(1L).build();
        MatchingApplication application = applicationWithStatus(100L, member, MatchingApplicationStatus.MATCHED);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.findByIdAndMemberIdWithLock(100L, member.getMemberId()))
                .willReturn(Optional.of(application));

        assertThatThrownBy(() -> matchingApplicationService.withdraw(member.getMemberId(), 100L))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.INVALID_APPLICATION_STATUS);
        verify(matchingTimePolicy, never()).now();
    }

    private SurveySubmission submittedSurvey(Member member) {
        return SurveySubmission.builder()
                .member(member)
                .status(SubmissionStatus.SUBMITTED)
                .agreeablenessScore(new BigDecimal("4.10"))
                .conscientiousnessScore(new BigDecimal("3.50"))
                .honestyHumilityScore(new BigDecimal("3.70"))
                .extroversionScore(new BigDecimal("3.20"))
                .goalPreferenceScore(new BigDecimal("5.00"))
                .workStyleScore(new BigDecimal("3.00"))
                .communicationStyleScore(new BigDecimal("1.00"))
                .extroversion2Score(new BigDecimal("3.00"))
                .extroversion3Score(new BigDecimal("3.00"))
                .extroversionType(ExtroversionType.A)
                .characterType(CharacterType.TRACK_RUNNER)
                .characterXScore(new BigDecimal("11.50"))
                .characterYScore(new BigDecimal("7.00"))
                .build();
    }

    private MatchingApplication waitingApplication(Long applicationId, Member member) {
        return applicationWithStatus(applicationId, member, MatchingApplicationStatus.WAITING);
    }

    private MatchingApplication applicationWithStatus(
            Long applicationId, Member member, MatchingApplicationStatus status) {
        return MatchingApplication.builder()
                .matchingApplicationId(applicationId)
                .member(member)
                .applicationDate(TODAY)
                .status(status)
                .contestCategory(InterestCategory.IT_AI_TECH)
                .leaderPreference(LeaderPreference.NEUTRAL)
                .skillScore(new BigDecimal("50.00"))
                .skillGroup(2)
                .collaborationDistance(100)
                .build();
    }
}
