package org.cotato.gongmozip.domains.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.review.dto.request.ReviewRequest.WriteReviewRequest;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewResultResponse;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewTargetListResponse;
import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.review.enums.ReviewAgreementLevel;
import org.cotato.gongmozip.domains.review.enums.ReviewKeyword;
import org.cotato.gongmozip.domains.review.exception.ReviewException;
import org.cotato.gongmozip.domains.review.exception.codes.ReviewErrorCode;
import org.cotato.gongmozip.domains.review.repository.ReviewRepository;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CollaborationPointService collaborationPointService;

    @Mock
    private ChatbotOrchestrationService chatbotOrchestrationService;

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private ReviewService reviewService;

    @DisplayName("SUBMITTED 상태가 아니면 리뷰 작성에 실패한다.")
    @Test
    void SUBMITTED_상태가_아니면_리뷰_작성에_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, defaultRequest(20L)))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.INVALID_TEAM_STATUS);
    }

    @DisplayName("팀원이 아니면 리뷰 작성에 실패한다.")
    @Test
    void 팀원이_아니면_리뷰_작성에_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, defaultRequest(20L)))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NOT_TEAM_MEMBER);
    }

    @DisplayName("리뷰 대상이 같은 팀의 활성 팀원이 아니면 실패한다.")
    @Test
    void 리뷰_대상이_같은_팀의_활성_팀원이_아니면_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, defaultRequest(999L)))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.NOT_TEAM_MEMBER);
    }

    @DisplayName("본인을 리뷰하면 실패한다.")
    @Test
    void 본인을_리뷰하면_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(10L)).willReturn(Optional.of(reviewer));

        // when & then
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, defaultRequest(10L)))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReviewErrorCode.CANNOT_REVIEW_SELF);
    }

    @DisplayName("이미 리뷰한 팀원이면 다시 작성할 수 없다.")
    @Test
    void 이미_리뷰한_팀원이면_다시_작성할_수_없다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember reviewee = teamMemberOf(team, 20L, "이해은");
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(20L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(1L, 10L, 20L))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, defaultRequest(20L)))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReviewErrorCode.ALREADY_REVIEWED);
    }

    @DisplayName("리뷰어가 아직 나머지 팀원을 다 리뷰하지 않았으면 포인트가 적립되지 않는다. (전원 완료 전이면 COMPLETED로 전이하지 않는다)")
    @Test
    void 리뷰어가_아직_나머지_팀원을_다_리뷰하지_않았으면_포인트가_적립되지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember reviewee = teamMemberOf(team, 20L, "이해은");
        TeamMember other = teamMemberOf(team, 30L, "박준수");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(20L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(1L, 10L, 20L))
                .willReturn(false);
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reviewer, reviewee, other));
        // 활성 팀원이 3명이라 reviewer는 총 2건을 써야 하는데, 지금까지 이 1건뿐이다.
        given(reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                        1L, reviewer.getTeamMemberId(), TeamMemberStatus.ACTIVE))
                .willReturn(1L);
        given(reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                        1L, TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE))
                .willReturn(1L);

        // when
        ReviewResultResponse response = reviewService.writeReview(1L, 10L, defaultRequest(20L));

        // then
        assertThat(response.revieweeTeamMemberId()).isEqualTo(20L);
        assertThat(response.communicationScore()).isEqualTo(ReviewAgreementLevel.AGREE);
        assertThat(response.keywords()).containsExactly(ReviewKeyword.TRUSTWORTHY);
        verify(collaborationPointService, never()).awardPoint(any(), any(), any());
        verify(chatbotOrchestrationService, never()).completeReview(any());
    }

    @DisplayName("리뷰어가 나머지 팀원 전체에 대한 리뷰를 방금 마치면 협업거리 포인트가 1회 적립된다.")
    @Test
    void 리뷰어가_나머지_팀원_전체에_대한_리뷰를_방금_마치면_협업거리_포인트가_1회_적립된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember reviewee = teamMemberOf(team, 20L, "이해은");
        TeamMember other = teamMemberOf(team, 30L, "박준수");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(20L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(1L, 10L, 20L))
                .willReturn(false);
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reviewer, reviewee, other));
        // 활성 팀원이 3명이라 reviewer는 총 2건을 써야 하는데, 이 리뷰가 그 2번째(마지막)다.
        given(reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                        1L, reviewer.getTeamMemberId(), TeamMemberStatus.ACTIVE))
                .willReturn(2L);
        given(reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                        1L, TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE))
                .willReturn(2L);

        // when
        reviewService.writeReview(1L, 10L, defaultRequest(20L));

        // then
        verify(collaborationPointService)
                .awardPoint(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN);
    }

    @DisplayName("이미 포인트를 지급받은 적이 있으면 조건을 다시 충족해도 중복 지급하지 않는다.")
    @Test
    void 이미_포인트를_지급받은_적이_있으면_조건을_다시_충족해도_중복_지급하지_않는다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember reviewee = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(20L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(1L, 10L, 20L))
                .willReturn(false);
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reviewer, reviewee));
        given(reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                        1L, reviewer.getTeamMemberId(), TeamMemberStatus.ACTIVE))
                .willReturn(1L);
        given(collaborationPointService.hasAwarded(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN))
                .willReturn(true);

        // when
        reviewService.writeReview(1L, 10L, defaultRequest(20L));

        // then
        verify(collaborationPointService, never()).awardPoint(any(), any(), any());
    }

    @DisplayName("리뷰 진행 중 팀원이 나가 남은 팀원이 이미 대상을 다 리뷰해뒀으면, 새로 리뷰를 쓰지 않아도 포인트가 지급된다.")
    @Test
    void 리뷰_진행_중_팀원이_나가_남은_팀원이_이미_대상을_다_리뷰해뒀으면_포인트가_지급된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember reviewee = teamMemberOf(team, 20L, "이해은");
        // C(30L)는 이미 나가서 findByTeamIdAndStatus(ACTIVE) 결과에는 더 이상 잡히지 않는다.

        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reviewer, reviewee));
        // reviewer는 C가 나가기 전 이미 reviewee 1명을 리뷰해뒀다 — 이제 활성 팀원 기준 필요
        // 리뷰(1건)를 이미 다 쓴 상태지만, 새로 리뷰를 쓴 적은 없어 writeReview는 호출되지 않는다.
        given(reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                        1L, reviewer.getTeamMemberId(), TeamMemberStatus.ACTIVE))
                .willReturn(1L);
        given(reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                        1L, reviewee.getTeamMemberId(), TeamMemberStatus.ACTIVE))
                .willReturn(0L);
        given(reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                        1L, TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE))
                .willReturn(1L);

        // when
        reviewService.recheckAfterMemberLeft(team);

        // then
        verify(collaborationPointService)
                .awardPoint(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN);
        verify(collaborationPointService, never()).awardPoint(eq(reviewee.getMember()), any(), any());
    }

    @DisplayName("이탈로 활성 팀원이 1명 이하로 줄면 더 쓸 리뷰가 없으므로 팀이 바로 COMPLETED로 전이된다.")
    @Test
    void 이탈로_활성_팀원이_1명_이하로_줄면_팀이_바로_COMPLETED로_전이된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember lastMember = teamMemberOf(team, 10L, "김철수");
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(lastMember));

        // when
        reviewService.recheckAfterMemberLeft(team);

        // then
        verify(chatbotOrchestrationService).completeReview(team);
    }

    @DisplayName("활성 팀원 전원이 서로를 다 리뷰하면 팀이 COMPLETED로 전이된다.")
    @Test
    void 활성_팀원_전원이_서로를_다_리뷰하면_팀이_COMPLETED로_전이된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember reviewee = teamMemberOf(team, 20L, "이해은");

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findById(20L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(1L, 10L, 20L))
                .willReturn(false);
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reviewer, reviewee));
        given(reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                        1L, reviewer.getTeamMemberId(), TeamMemberStatus.ACTIVE))
                .willReturn(1L);
        given(reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                        1L, TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE))
                .willReturn(2L);

        // when
        reviewService.writeReview(1L, 10L, defaultRequest(20L));

        // then
        verify(chatbotOrchestrationService).completeReview(team);
    }

    @DisplayName("리뷰 대상 목록을 조회하면 나를 제외한 활성 팀원이 반환되고, 이미 리뷰한 대상은 표시된다.")
    @Test
    void 리뷰_대상_목록을_조회하면_나를_제외한_활성_팀원이_반환되고_이미_리뷰한_대상은_표시된다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.SUBMITTED).build();
        TeamMember reviewer = teamMemberOf(team, 10L, "김철수");
        TeamMember alreadyReviewed = teamMemberOf(team, 20L, "이해은");
        TeamMember notYetReviewed = teamMemberOf(team, 30L, "박준수");
        MemberAvatarResponse avatar =
                new MemberAvatarResponse(30L, CharacterType.LEAD_RUNNER, CharacterPalette.DEFAULT, null, null);

        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(teamMemberRepository.findByTeam_TeamIdAndMember_MemberId(1L, 10L)).willReturn(Optional.of(reviewer));
        given(teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE))
                .willReturn(List.of(reviewer, alreadyReviewed, notYetReviewed));
        given(reviewRepository.findByTeam_TeamIdAndReviewer_TeamMemberId(1L, reviewer.getTeamMemberId()))
                .willReturn(List.of(Review.builder()
                        .team(team)
                        .reviewer(reviewer)
                        .reviewee(alreadyReviewed)
                        .communicationScore(ReviewAgreementLevel.AGREE)
                        .participationScore(ReviewAgreementLevel.AGREE)
                        .keywords(List.of(ReviewKeyword.TRUSTWORTHY.name()))
                        .build()));
        given(characterService.findAvatarsByMembers(List.of(alreadyReviewed.getMember(), notYetReviewed.getMember())))
                .willReturn(Map.of(30L, avatar));

        // when
        ReviewTargetListResponse response = reviewService.getReviewTargets(1L, 10L);

        // then
        assertThat(response.targets()).hasSize(2);
        assertThat(response.targets())
                .filteredOn(t -> t.teamMemberId().equals(20L))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.alreadyReviewed()).isTrue();
                    assertThat(t.avatar()).isNull();
                });
        assertThat(response.targets())
                .filteredOn(t -> t.teamMemberId().equals(30L))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.alreadyReviewed()).isFalse();
                    assertThat(t.avatar()).isEqualTo(avatar);
                });
    }

    @DisplayName("존재하지 않는 팀의 리뷰 대상을 조회하면 예외가 발생한다.")
    @Test
    void 존재하지_않는_팀의_리뷰_대상을_조회하면_예외가_발생한다() {
        // given
        given(teamRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewTargets(999L, 10L))
                .isInstanceOf(TeamException.class)
                .hasFieldOrPropertyWithValue("errorCode", TeamErrorCode.TEAM_NOT_FOUND);
    }

    private WriteReviewRequest defaultRequest(Long revieweeTeamMemberId) {
        return new WriteReviewRequest(
                revieweeTeamMemberId,
                ReviewAgreementLevel.AGREE,
                ReviewAgreementLevel.AGREE,
                List.of(ReviewKeyword.TRUSTWORTHY));
    }

    private TeamMember teamMemberOf(Team team, Long memberId, String nickname) {
        Member member = Member.builder().memberId(memberId).build();
        Profile profile = Profile.builder().nickname(nickname).build();
        return TeamMember.builder()
                .teamMemberId(memberId)
                .team(team)
                .member(member)
                .profile(profile)
                .status(TeamMemberStatus.ACTIVE)
                .build();
    }
}
