package org.cotato.gongmozip.domains.contest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.contest.dto.response.RecommendationResponse.RecommendationReasonResponse;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.enums.ContestStatus;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ContestRecommendationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private SurveySubmissionRepository surveySubmissionRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private org.cotato.gongmozip.domains.team.repository.TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private ContestRecommendationService contestRecommendationService;

    @DisplayName("추천 목록에 포함되지 않은 공모전의 추천 사유를 조회하면 예외가 발생한다.")
    @Test
    void 추천_목록에_포함되지_않은_공모전_사유_조회시_예외가_발생한다() {
        // given
        Long memberId = 1L;
        Long contestId = 99L;
        Member member = Member.builder().memberId(memberId).build();
        Contest contest = Contest.builder()
                .contestId(contestId)
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .applyStartAt(LocalDateTime.now())
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();

        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));
        given(profileRepository.findAllByMemberOrderByUpdatedAtDesc(member))
                .willReturn(Collections.singletonList(profile));

        given(contestRepository.findAllWithFilterAndDeadlineAsc(
                        any(),
                        any(),
                        any(),
                        any(LocalDateTime.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(Collections.emptyList()));
        given(aiClient.recommendContests(eq(InterestCategory.IT_AI_TECH), any(), any()))
                .willReturn(Collections.emptyList());
        given(teamMemberRepository.findCompletedProjectsAll(any(), any(), any()))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> contestRecommendationService.getRecommendationReason(contestId, memberId))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.NOT_RECOMMENDED_CONTEST.getMessage());
    }

    @DisplayName("추천된 공모전의 추천 사유 조회 시 프로필 관심 분야와 설문조사 캐릭터 정보를 활용해 사유를 생성한다.")
    @Test
    void 추천된_공모전의_추천_사유를_정상_반환한다() {
        // given
        Long memberId = 1L;
        Long contestId = 2L;
        Member member = Member.builder().memberId(memberId).build();
        Contest contest = Contest.builder()
                .contestId(contestId)
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .applyStartAt(LocalDateTime.now())
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();

        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .build();

        SurveySubmission submission = SurveySubmission.builder()
                .member(member)
                .status(SubmissionStatus.SUBMITTED)
                .characterType(CharacterType.LEAD_RUNNER)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));
        given(profileRepository.findAllByMemberOrderByUpdatedAtDesc(member))
                .willReturn(Collections.singletonList(profile));

        given(contestRepository.findAllWithFilterAndDeadlineAsc(
                        any(),
                        any(),
                        any(),
                        any(LocalDateTime.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(Collections.singletonList(contest)));
        given(aiClient.recommendContests(eq(InterestCategory.IT_AI_TECH), any(), any()))
                .willReturn(Collections.singletonList(contestId));
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));
        given(teamMemberRepository.findCompletedProjectsAll(any(), any(), any()))
                .willReturn(List.of());

        // when
        RecommendationReasonResponse response =
                contestRecommendationService.getRecommendationReason(contestId, memberId);

        // then
        assertThat(response.reason()).contains("IT/AI/기술");
        assertThat(response.reason()).contains("리드러너");
    }

    @DisplayName("추천된 공모전의 추천 사유 조회 시 완주 경험이 있으면 완주 경험 문구를 포함하여 사유를 생성한다.")
    @Test
    void 추천된_공모전의_추천_사유_조회시_완주_경험이_있으면_문구에_포함된다() {
        // given
        Long memberId = 1L;
        Long contestId = 2L;
        Member member = Member.builder().memberId(memberId).build();
        Contest contest = Contest.builder()
                .contestId(contestId)
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .applyStartAt(LocalDateTime.now())
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();

        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .build();

        SurveySubmission submission = SurveySubmission.builder()
                .member(member)
                .status(SubmissionStatus.SUBMITTED)
                .characterType(CharacterType.LEAD_RUNNER)
                .build();

        org.cotato.gongmozip.domains.contest.entity.Contest completedContest =
                org.cotato.gongmozip.domains.contest.entity.Contest.builder()
                        .contestId(20L)
                        .title("완주한 AI 해커톤")
                        .build();

        org.cotato.gongmozip.domains.team.entity.Team completedTeam =
                org.cotato.gongmozip.domains.team.entity.Team.builder()
                        .teamId(30L)
                        .status(org.cotato.gongmozip.domains.team.enums.TeamStatus.COMPLETED)
                        .contest(completedContest)
                        .build();

        org.cotato.gongmozip.domains.team.entity.TeamMember completedMember =
                org.cotato.gongmozip.domains.team.entity.TeamMember.builder()
                        .team(completedTeam)
                        .member(member)
                        .profile(profile)
                        .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(contestRepository.findById(contestId)).willReturn(Optional.of(contest));
        given(profileRepository.findAllByMemberOrderByUpdatedAtDesc(member))
                .willReturn(Collections.singletonList(profile));

        given(contestRepository.findAllWithFilterAndDeadlineAsc(
                        any(),
                        any(),
                        any(),
                        any(LocalDateTime.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(Collections.singletonList(contest)));
        given(aiClient.recommendContests(eq(InterestCategory.IT_AI_TECH), any(), any()))
                .willReturn(Collections.singletonList(contestId));
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));
        given(teamMemberRepository.findCompletedProjectsAll(eq(memberId), any(), any()))
                .willReturn(List.of(completedMember));

        // when
        RecommendationReasonResponse response =
                contestRecommendationService.getRecommendationReason(contestId, memberId);

        // then
        assertThat(response.reason()).contains("IT/AI/기술");
        assertThat(response.reason()).contains("리드러너");
        assertThat(response.reason()).contains("이전 완주 프로젝트인 '완주한 AI 해커톤' 등의 경험을 바탕으로");
    }
}
