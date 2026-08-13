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

    @DisplayName("추천된 공모전의 추천 사유 조회 시 삭제(숨김)된 완주 프로젝트는 추천 사유 생성 시 포함되지 않는다.")
    @Test
    void 추천된_공모전의_추천_사유_조회시_삭제된_완주_경험은_문구에_제외된다() {
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
        // Repository에서 삭제(숨김) 플래그 필터로 인해 조회되지 않는 것으로 모킹 (빈 리스트 반환)
        given(teamMemberRepository.findCompletedProjectsAll(eq(memberId), any(), any()))
                .willReturn(List.of());

        // when
        RecommendationReasonResponse response =
                contestRecommendationService.getRecommendationReason(contestId, memberId);

        // then
        assertThat(response.reason()).contains("IT/AI/기술");
        assertThat(response.reason()).contains("리드러너");
        assertThat(response.reason()).doesNotContain("이전 완주 프로젝트인");
    }

    @DisplayName("유저의 프로필이 없는 경우 최근 등록된 오픈 공모전 중 최대 3개를 추천한다.")
    @Test
    void 유저의_프로필이_없는_경우_최근_등록된_공모전을_추천한다() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(profileRepository.findAllByMemberOrderByUpdatedAtDesc(member)).willReturn(Collections.emptyList());

        Contest contest1 = Contest.builder()
                .contestId(1L)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        Contest contest2 = Contest.builder()
                .contestId(2L)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        Contest contest3 = Contest.builder()
                .contestId(3L)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        Contest contest4 = Contest.builder()
                .contestId(4L)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        List<Contest> mockContests = List.of(contest1, contest2, contest3, contest4);

        given(contestRepository.findAllWithFilterAndNewest(
                        any(),
                        any(),
                        any(),
                        any(LocalDateTime.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(mockContests));

        // when
        List<org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestSummaryResponse> response =
                contestRecommendationService.getHomeRecommendations(memberId);

        // then
        assertThat(response).hasSize(3);
    }

    @DisplayName("선호 카테고리 공모전이 3개 미만인 경우 전체 최신 공모전으로 채워서 추천한다.")
    @Test
    void 선호_카테고리_공모전이_부족한_경우_전체_공모전으로_채운다() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder()
                .profileId(10L)
                .member(member)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(profileRepository.findAllByMemberOrderByUpdatedAtDesc(member))
                .willReturn(Collections.singletonList(profile));

        Contest contest1 = Contest.builder()
                .contestId(1L)
                .title("컨테스트1")
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        Contest contest2 = Contest.builder()
                .contestId(2L)
                .title("컨테스트2")
                .category(InterestCategory.MARKETING_AD_BRANDING)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        Contest contest3 = Contest.builder()
                .contestId(3L)
                .title("컨테스트3")
                .category(InterestCategory.ART_DESIGN)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();

        // 선호 카테고리는 1개만 조회됨
        given(contestRepository.findAllWithFilterAndDeadlineAsc(
                        any(),
                        eq(InterestCategory.IT_AI_TECH),
                        any(),
                        any(LocalDateTime.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(List.of(contest1)));

        // 폴백 조회를 통해 전체 최신 오픈 공모전들을 조회함
        given(contestRepository.findAllWithFilterAndNewest(
                        any(),
                        any(),
                        any(),
                        any(LocalDateTime.class),
                        any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(List.of(contest1, contest2, contest3)));

        given(teamMemberRepository.findCompletedProjectsAll(any(), any(), any()))
                .willReturn(List.of());

        // AI client가 1, 2, 3번 공모전을 추천 리스트로 반환함
        given(aiClient.recommendContests(eq(InterestCategory.IT_AI_TECH), eq(List.of(1L, 2L, 3L)), any()))
                .willReturn(List.of(1L, 2L, 3L));

        // when
        List<org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.ContestSummaryResponse> response =
                contestRecommendationService.getHomeRecommendations(memberId);

        // then
        assertThat(response).hasSize(3);
        assertThat(response.stream().map(r -> r.contestId()).toList()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }
}
