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
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.matching.enums.MatchingApplicationStatus;
import org.cotato.gongmozip.domains.matching.enums.WithdrawalType;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.MatchingApplicationRepository;
import org.cotato.gongmozip.domains.matching.score.ProjectScoreProvider;
import org.cotato.gongmozip.domains.matching.score.SkillScoreCalculator;
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
                collaborationPointHistoryRepository,
                collaborationPointService,
                projectScoreProvider,
                new SkillScoreCalculator(),
                matchingTimePolicy);
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
        given(matchingTimePolicy.today()).willReturn(TODAY);
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
        assertThat(response.skillGroup()).isEqualTo(2);
    }

    @DisplayName("같은 날 취소 이력이 있어도 다시 신청할 수 없다.")
    @Test
    void duplicateApplicationIsRejected() {
        Member member = Member.builder().memberId(1L).build();
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.today()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TODAY))
                .willReturn(true);

        assertThatThrownBy(() -> matchingApplicationService.apply(member.getMemberId(), request))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.ALREADY_APPLIED_TODAY);
    }

    @DisplayName("작성한 프로필이 없으면 매칭을 신청할 수 없다.")
    @Test
    void applicationWithoutProfileIsRejected() {
        Member member = Member.builder().memberId(1L).build();
        ApplyRequest request = new ApplyRequest(10L, InterestCategory.IT_AI_TECH, LeaderPreference.NEUTRAL, true);
        given(matchingTimePolicy.isApplicationOpen()).willReturn(true);
        given(matchingTimePolicy.today()).willReturn(TODAY);
        given(matchingTimePolicy.now()).willReturn(NOW);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.existsByMemberAndApplicationDate(member, TODAY))
                .willReturn(false);
        given(profileRepository.countByMember(member)).willReturn(0);

        assertThatThrownBy(() -> matchingApplicationService.apply(member.getMemberId(), request))
                .isInstanceOf(MatchingException.class)
                .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.PROFILE_REQUIRED);
    }

    @DisplayName("14시 전 철회는 무료 취소로 처리한다.")
    @Test
    void withdrawBeforeDeadlineIsFreeCancel() {
        Member member = Member.builder().memberId(1L).collaborationPoint(100).build();
        MatchingApplication application = waitingApplication(100L, member);
        given(memberRepository.findByIdWithLock(member.getMemberId())).willReturn(Optional.of(member));
        given(matchingApplicationRepository.findByIdWithLock(100L)).willReturn(Optional.of(application));
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
        given(matchingApplicationRepository.findByIdWithLock(100L)).willReturn(Optional.of(application));
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
        return MatchingApplication.builder()
                .matchingApplicationId(applicationId)
                .member(member)
                .applicationDate(TODAY)
                .status(MatchingApplicationStatus.WAITING)
                .build();
    }
}
