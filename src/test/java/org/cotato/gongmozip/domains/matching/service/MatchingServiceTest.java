package org.cotato.gongmozip.domains.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderCandidateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.LeaderRecommendationDetailResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingExplanationResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonCreateResponse;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.MatchingReasonDetailResponse;
import org.cotato.gongmozip.domains.matching.entity.LeaderRecommendation;
import org.cotato.gongmozip.domains.matching.entity.MatchingExplanation;
import org.cotato.gongmozip.domains.matching.entity.MatchingGroup;
import org.cotato.gongmozip.domains.matching.entity.MatchingReason;
import org.cotato.gongmozip.domains.matching.enums.MatchingAiStatus;
import org.cotato.gongmozip.domains.matching.enums.MatchingGroupStatus;
import org.cotato.gongmozip.domains.matching.exception.MatchingException;
import org.cotato.gongmozip.domains.matching.exception.codes.MatchingErrorCode;
import org.cotato.gongmozip.domains.matching.repository.LeaderRecommendationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingExplanationRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupMemberRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingGroupRepository;
import org.cotato.gongmozip.domains.matching.repository.MatchingReasonRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MatchingExplanationRepository matchingExplanationRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingGroupMemberRepository matchingGroupMemberRepository;

    @Mock
    private MatchingReasonRepository matchingReasonRepository;

    @Mock
    private LeaderRecommendationRepository leaderRecommendationRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private MatchingAiWorker matchingAiWorker;

    @InjectMocks
    private MatchingService matchingService;

    private Member member;
    private Member otherMember;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(1L).email("test@gongmozip.com").build();
        otherMember = Member.builder().memberId(2L).email("other@gongmozip.com").build();
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @DisplayName("AI 분석 매칭 설명 조회 시 성공한다")
    @Test
    void AI_분석_매칭_설명_조회_시_성공한다() {
        // given
        MatchingExplanation explanation = MatchingExplanation.builder()
                .title("설명 제목")
                .summary("설명 요약")
                .sections(new ArrayList<>())
                .disclaimer("참고 사항")
                .build();
        given(matchingExplanationRepository.findFirstByOrderByCreatedAtDesc()).willReturn(Optional.of(explanation));

        // when
        MatchingExplanationResponse response = matchingService.getMatchingExplanation();

        // then
        assertThat(response.title()).isEqualTo("설명 제목");
        assertThat(response.summary()).isEqualTo("설명 요약");
    }

    @DisplayName("AI 매칭 추천 사유 생성 요청 시 성공 접수된다")
    @Test
    void AI_매칭_추천_사유_생성_요청_시_성공_접수된다() {
        // given
        MatchingGroup group = MatchingGroup.builder()
                .matchingGroupId(10L)
                .category(InterestCategory.IT_AI_TECH)
                .skillGroup(1)
                .matchingScore(BigDecimal.valueOf(80.5))
                .status(MatchingGroupStatus.COMPLETED)
                .build();

        MatchingReason reason = MatchingReason.builder()
                .matchingReasonId(20L)
                .matchingGroup(group)
                .status(MatchingAiStatus.PENDING)
                .build();

        given(matchingGroupRepository.findById(10L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.existsByMatchingGroupAndMember(group, member))
                .willReturn(true);
        given(matchingReasonRepository.findByMatchingGroup(group)).willReturn(Optional.empty());
        given(matchingReasonRepository.save(any(MatchingReason.class))).willReturn(reason);

        // when
        MatchingReasonCreateResponse response = matchingService.createMatchingReason(10L, member);

        // then
        assertThat(response.reasonId()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo("PENDING");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        then(matchingAiWorker).should().generateMatchingReasonAsync(20L);
    }

    @DisplayName("AI 매칭 추천 사유 생성 중복 요청 시 예외가 발생한다")
    @Test
    void AI_매칭_추천_사유_생성_중복_요청_시_예외가_발생한다() {
        // given
        MatchingGroup group = MatchingGroup.builder()
                .matchingGroupId(10L)
                .category(InterestCategory.IT_AI_TECH)
                .skillGroup(1)
                .status(MatchingGroupStatus.COMPLETED)
                .build();

        MatchingReason reason = MatchingReason.builder()
                .matchingReasonId(20L)
                .matchingGroup(group)
                .status(MatchingAiStatus.PROCESSING)
                .build();

        given(matchingGroupRepository.findById(10L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.existsByMatchingGroupAndMember(group, member))
                .willReturn(true);
        given(matchingReasonRepository.findByMatchingGroup(group)).willReturn(Optional.of(reason));

        // when & then
        assertThatThrownBy(() -> matchingService.createMatchingReason(10L, member))
                .isInstanceOf(MatchingException.class)
                .hasMessage(MatchingErrorCode.MATCHING_REASON_IN_PROGRESS.getMessage());
    }

    @DisplayName("매칭 추천 사유 조회 시 성공한다")
    @Test
    void 매칭_추천_사유_조회_시_성공한다() {
        // given
        MatchingGroup group = MatchingGroup.builder().matchingGroupId(10L).build();
        MatchingReason reason = MatchingReason.builder()
                .matchingReasonId(20L)
                .matchingGroup(group)
                .status(MatchingAiStatus.COMPLETED)
                .headline("헤드라인")
                .summary("요약")
                .build();

        given(matchingGroupRepository.findById(10L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.existsByMatchingGroupAndMember(group, member))
                .willReturn(true);
        given(matchingReasonRepository.findByMatchingGroup(group)).willReturn(Optional.of(reason));

        // when
        MatchingReasonDetailResponse response = matchingService.getMatchingReason(10L, member);

        // then
        assertThat(response.reasonId()).isEqualTo(20L);
        assertThat(response.headline()).isEqualTo("헤드라인");
        assertThat(response.summary()).isEqualTo("요약");
    }

    @DisplayName("AI 팀장 추천 생성 요청 시 성공 접수된다")
    @Test
    void AI_팀장_추천_생성_요청_시_성공_접수된다() {
        // given
        Team team = Team.builder().teamId(30L).build();
        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .member(member)
                .status(TeamMemberStatus.ACTIVE)
                .build();

        LeaderRecommendation rec = LeaderRecommendation.builder()
                .leaderRecommendationId(40L)
                .team(team)
                .status(MatchingAiStatus.PENDING)
                .build();

        given(teamRepository.findById(30L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(30L, 1L)).willReturn(Optional.of(teamMember));
        given(leaderRecommendationRepository.findByTeam(team)).willReturn(Optional.empty());
        given(leaderRecommendationRepository.save(any(LeaderRecommendation.class)))
                .willReturn(rec);

        // when
        LeaderRecommendationCreateResponse response = matchingService.createLeaderRecommendation(30L, member);

        // then
        assertThat(response.recommendationId()).isEqualTo(40L);
        assertThat(response.status()).isEqualTo("PENDING");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        then(matchingAiWorker).should().generateLeaderRecommendationAsync(40L);
    }

    @DisplayName("AI 팀장 추천 조회 시 성공한다")
    @Test
    void AI_팀장_추천_조회_시_성공한다() {
        // given
        Team team = Team.builder().teamId(30L).build();
        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .member(member)
                .status(TeamMemberStatus.ACTIVE)
                .build();

        Profile profile = Profile.builder().nickname("채영").build();
        TeamMember activeTeamMember =
                TeamMember.builder().member(member).profile(profile).build();

        LeaderRecommendation rec = LeaderRecommendation.builder()
                .leaderRecommendationId(40L)
                .team(team)
                .status(MatchingAiStatus.COMPLETED)
                .recommendedMember(member)
                .recommendationReason("이유")
                .candidates(List.of(new LeaderCandidateResponse(1L, "채영", 1, 90, "추천이유")))
                .build();

        given(teamRepository.findById(30L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(30L, 1L)).willReturn(Optional.of(teamMember));
        given(leaderRecommendationRepository.findByTeam(team)).willReturn(Optional.of(rec));
        given(teamMemberRepository.findByTeamIdAndStatus(30L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(activeTeamMember));

        // when
        LeaderRecommendationDetailResponse response = matchingService.getLeaderRecommendation(30L, member);

        // then
        assertThat(response.recommendationId()).isEqualTo(40L);
        assertThat(response.recommendedMemberNickname()).isEqualTo("채영");
        assertThat(response.candidates().get(0).nickname()).isEqualTo("채영");
    }

    @DisplayName("소속 멤버가 아닌 경우 매칭 추천 사유 생성 요청 시 예외가 발생한다")
    @Test
    void 소속_멤버가_아닌_경우_매칭_추천_사유_생성_요청_시_예외가_발생한다() {
        // given
        MatchingGroup group = MatchingGroup.builder()
                .matchingGroupId(10L)
                .category(InterestCategory.IT_AI_TECH)
                .skillGroup(1)
                .status(MatchingGroupStatus.COMPLETED)
                .build();

        given(matchingGroupRepository.findById(10L)).willReturn(Optional.of(group));
        given(matchingGroupMemberRepository.existsByMatchingGroupAndMember(group, otherMember))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> matchingService.createMatchingReason(10L, otherMember))
                .isInstanceOf(MatchingException.class)
                .hasMessage(MatchingErrorCode.MATCHING_GROUP_ACCESS_DENIED.getMessage());
    }

    @DisplayName("활성 팀원이 아닌 경우 팀장 추천 생성 요청 시 예외가 발생한다")
    @Test
    void 활성_팀원이_아닌_경우_팀장_추천_생성_요청_시_예외가_발생한다() {
        // given
        Team team = Team.builder().teamId(30L).build();
        TeamMember inactiveMember = TeamMember.builder()
                .team(team)
                .member(otherMember)
                .status(TeamMemberStatus.LEFT)
                .build();

        given(teamRepository.findById(30L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(30L, 2L))
                .willReturn(Optional.of(inactiveMember));

        // when & then
        assertThatThrownBy(() -> matchingService.createLeaderRecommendation(30L, otherMember))
                .isInstanceOf(MatchingException.class)
                .hasMessage(MatchingErrorCode.TEAM_ACCESS_DENIED.getMessage());
    }

    @DisplayName("AI 팀장 추천 이미 진행 중일 때 생성 요청 시 예외가 발생한다")
    @Test
    void AI_팀장_추천_이미_진행_중일_때_생성_요청_시_예외가_발생한다() {
        // given
        Team team = Team.builder().teamId(30L).build();
        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .member(member)
                .status(TeamMemberStatus.ACTIVE)
                .build();

        LeaderRecommendation rec = LeaderRecommendation.builder()
                .leaderRecommendationId(40L)
                .team(team)
                .status(MatchingAiStatus.PROCESSING)
                .build();

        given(teamRepository.findById(30L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(30L, 1L)).willReturn(Optional.of(teamMember));
        given(leaderRecommendationRepository.findByTeam(team)).willReturn(Optional.of(rec));

        // when & then
        assertThatThrownBy(() -> matchingService.createLeaderRecommendation(30L, member))
                .isInstanceOf(MatchingException.class)
                .hasMessage(MatchingErrorCode.LEADER_RECOMMENDATION_IN_PROGRESS.getMessage());
    }
}
