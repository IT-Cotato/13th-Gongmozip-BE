package org.cotato.gongmozip.domains.review.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.review.converter.ReviewConverter;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀원 리뷰 작성 (Phase 9, docs/decisions/09-review.md). 팀이 SUBMITTED 상태인 동안에만
 * 작성 가능하며, 활성 팀원끼리 서로(자기 자신 제외) 1회씩 작성한다. 전원이 서로를 다 리뷰하면
 * 자동으로 팀이 COMPLETED로 전이된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CollaborationPointService collaborationPointService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;

    @Transactional
    public ReviewResultResponse writeReview(Long teamId, Long reviewerMemberId, WriteReviewRequest request) {
        Team team = requireTeamSubmitted(teamId);
        TeamMember reviewer = requireActiveMember(teamId, reviewerMemberId);
        TeamMember reviewee = requireActiveTeamMember(teamId, request.revieweeTeamMemberId());

        if (reviewer.getTeamMemberId().equals(reviewee.getTeamMemberId())) {
            throw new ReviewException(ReviewErrorCode.CANNOT_REVIEW_SELF);
        }
        if (reviewRepository.existsByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_TeamMemberId(
                teamId, reviewer.getTeamMemberId(), reviewee.getTeamMemberId())) {
            throw new ReviewException(ReviewErrorCode.ALREADY_REVIEWED);
        }

        Review saved = reviewRepository.save(ReviewConverter.toReview(team, reviewer, reviewee, request.content()));
        List<TeamMember> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE);
        awardPointIfReviewerJustCompleted(team, reviewer, activeMembers);
        completeReviewIfAllDone(team, activeMembers);

        return ReviewConverter.toReviewResultResponse(saved);
    }

    /**
     * 팀원이 리뷰 진행 중(SUBMITTED) 나갔을 때(TeamService.leaveTeam) 호출된다. 남은 활성
     * 팀원들이 이미 서로에 대한 리뷰를 모두 마쳤더라도, 이 완료 확인은 원래 새 리뷰가 제출되는
     * 시점에만 실행되므로 나간 사람 때문에 막혀있던 완료 조건이 뒤늦게 충족돼도 아무도 다시
     * 확인하지 않는다. 나가기 이후 기준으로 즉시 재확인한다.
     */
    @Transactional
    public void recheckAfterMemberLeft(Team team) {
        if (team.getStatus() != TeamStatus.SUBMITTED) {
            return;
        }
        List<TeamMember> activeMembers =
                teamMemberRepository.findByTeamIdAndStatus(team.getTeamId(), TeamMemberStatus.ACTIVE);
        completeReviewIfAllDone(team, activeMembers);
    }

    // 리뷰어 본인이 나머지 활성 팀원 전원에 대한 리뷰를 방금 다 썼을 때만(=이 리뷰가 그 마지막
    // 리뷰일 때만) 협업거리 포인트를 1회 지급한다. 리뷰 1건마다 지급하면 안 된다
    // (기능명세서 5.1.3.6.1 — 리뷰 최종 완료 시점에 총 10m를 딱 한 번 지급).
    private void awardPointIfReviewerJustCompleted(Team team, TeamMember reviewer, List<TeamMember> activeMembers) {
        long expectedReviewsByReviewer = activeMembers.size() - 1L;
        if (expectedReviewsByReviewer <= 0) {
            return;
        }

        long writtenByReviewer = reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberId(
                team.getTeamId(), reviewer.getTeamMemberId());
        if (writtenByReviewer == expectedReviewsByReviewer) {
            collaborationPointService.awardPoint(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN);
        }
    }

    // 활성 팀원 전원이 서로(자기 자신 제외)를 리뷰했으면 팀을 COMPLETED로 전이시킨다.
    private void completeReviewIfAllDone(Team team, List<TeamMember> activeMembers) {
        long expectedPairs = (long) activeMembers.size() * (activeMembers.size() - 1);
        if (expectedPairs == 0) {
            return;
        }

        long writtenPairs = reviewRepository.countByTeam_TeamIdAndReviewer_StatusAndReviewee_Status(
                team.getTeamId(), TeamMemberStatus.ACTIVE, TeamMemberStatus.ACTIVE);
        if (writtenPairs >= expectedPairs) {
            chatbotOrchestrationService.completeReview(team);
        }
    }

    private Team requireTeamSubmitted(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.SUBMITTED) {
            throw new TeamException(TeamErrorCode.INVALID_TEAM_STATUS);
        }
        return team;
    }

    private TeamMember requireActiveMember(Long teamId, Long memberId) {
        return teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .filter(teamMember -> teamMember.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));
    }

    private TeamMember requireActiveTeamMember(Long teamId, Long teamMemberId) {
        return teamMemberRepository
                .findById(teamMemberId)
                .filter(teamMember -> teamMember.getTeam().getTeamId().equals(teamId))
                .filter(teamMember -> teamMember.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));
    }
}
