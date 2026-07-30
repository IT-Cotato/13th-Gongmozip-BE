package org.cotato.gongmozip.domains.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.review.dto.request.ReviewRequest.WriteReviewRequest;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewResultResponse;
import org.cotato.gongmozip.domains.review.entity.Review;
import org.cotato.gongmozip.domains.review.exception.ReviewException;
import org.cotato.gongmozip.domains.review.exception.codes.ReviewErrorCode;
import org.cotato.gongmozip.domains.review.repository.ReviewRepository;
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

    @InjectMocks
    private ReviewService reviewService;

    @DisplayName("SUBMITTED 상태가 아니면 리뷰 작성에 실패한다.")
    @Test
    void SUBMITTED_상태가_아니면_리뷰_작성에_실패한다() {
        // given
        Team team = Team.builder().teamId(1L).status(TeamStatus.IN_PROGRESS).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        // when & then
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, new WriteReviewRequest(20L, "잘했어요")))
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
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, new WriteReviewRequest(20L, "잘했어요")))
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
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, new WriteReviewRequest(999L, "잘했어요")))
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
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, new WriteReviewRequest(10L, "저는 훌륭해요")))
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
        assertThatThrownBy(() -> reviewService.writeReview(1L, 10L, new WriteReviewRequest(20L, "잘했어요")))
                .isInstanceOf(ReviewException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReviewErrorCode.ALREADY_REVIEWED);
    }

    @DisplayName("정상적으로 리뷰를 작성하면 협업거리 포인트가 적립된다. (전원 완료 전이면 COMPLETED로 전이하지 않는다)")
    @Test
    void 정상적으로_리뷰를_작성하면_협업거리_포인트가_적립된다() {
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
        given(reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                        1L, TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE))
                .willReturn(1L);

        // when
        ReviewResultResponse response = reviewService.writeReview(1L, 10L, new WriteReviewRequest(20L, "잘했어요"));

        // then
        assertThat(response.revieweeTeamMemberId()).isEqualTo(20L);
        assertThat(response.content()).isEqualTo("잘했어요");
        verify(collaborationPointService)
                .awardPoint(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN);
        verify(chatbotOrchestrationService, never()).completeReview(any());
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
        given(reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                        1L, TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE))
                .willReturn(2L);

        // when
        reviewService.writeReview(1L, 10L, new WriteReviewRequest(20L, "잘했어요"));

        // then
        verify(chatbotOrchestrationService).completeReview(team);
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
