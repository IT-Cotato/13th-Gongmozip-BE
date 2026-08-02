package org.cotato.gongmozip.domains.review.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.MemberAvatarResponse;
import org.cotato.gongmozip.domains.character.service.CharacterService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.review.converter.ReviewConverter;
import org.cotato.gongmozip.domains.review.dto.request.ReviewRequest.WriteReviewRequest;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewResultResponse;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewTargetListResponse;
import org.cotato.gongmozip.domains.review.dto.response.ReviewResponse.ReviewTargetResponse;
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
    private final CharacterService characterService;

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
     * 리뷰할 팀원 목록을 조회한다. 활성 팀원 중 나를 제외한 전원을 반환하며, 이미 리뷰를 쓴
     * 대상은 {@code alreadyReviewed=true}로 표시한다 — 프론트가 리뷰 대상 선택 화면에서 이미
     * 작성한 팀원을 비활성화해서 보여줄 수 있도록 하기 위함(기존에는 조회 API가 없어 프론트가
     * 중복 제출을 시도해봐야만 알 수 있었음).
     */
    public ReviewTargetListResponse getReviewTargets(Long teamId, Long reviewerMemberId) {
        teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        TeamMember reviewer = requireActiveMember(teamId, reviewerMemberId);

        List<TeamMember> targets = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE).stream()
                .filter(teamMember -> !teamMember.getTeamMemberId().equals(reviewer.getTeamMemberId()))
                .toList();

        Set<Long> reviewedRevieweeIds =
                reviewRepository.findByTeam_TeamIdAndReviewer_TeamMemberId(teamId, reviewer.getTeamMemberId()).stream()
                        .map(review -> review.getReviewee().getTeamMemberId())
                        .collect(Collectors.toSet());
        Map<Long, MemberAvatarResponse> avatarsByMemberId = characterService.findAvatarsByMembers(
                targets.stream().map(TeamMember::getMember).toList());

        List<ReviewTargetResponse> responses = targets.stream()
                .map(target -> ReviewConverter.toReviewTargetResponse(
                        target,
                        reviewedRevieweeIds.contains(target.getTeamMemberId()),
                        avatarsByMemberId.get(target.getMember().getMemberId())))
                .toList();

        return new ReviewTargetListResponse(responses);
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
        // 나간 사람 때문에 "남은 활성 팀원 전원"의 정의(분모)가 줄어들면, 이미 그 줄어든 대상을
        // 전부 리뷰해뒀던 팀원은 새로 리뷰를 쓰지 않고도 지급 조건을 방금 막 충족한 셈이 된다
        // (writeReview 안에서만 지급을 확인하던 기존 구조로는 이 경우를 놓친다). 남은 팀원 전원을
        // 다시 확인한다 — awardPointIfReviewerJustCompleted 자체가 중복 지급 방지 가드를 갖고
        // 있어(collaborationPointService.hasAwarded) 여러 번 나가도 안전하다.
        for (TeamMember member : activeMembers) {
            awardPointIfReviewerJustCompleted(team, member, activeMembers);
        }
        completeReviewIfAllDone(team, activeMembers);
    }

    // 리뷰어 본인이 나머지 활성 팀원 전원에 대한 리뷰를 다 썼으면 협업거리 포인트를 1회 지급한다
    // (기능명세서 5.1.3.6.1 — 리뷰 최종 완료 시점에 총 10m를 딱 한 번 지급). writeReview 호출
    // 시점뿐 아니라 recheckAfterMemberLeft에서도 여러 번 호출될 수 있어, 이미 지급된 적이
    // 있는지 collaborationPointService.hasAwarded로 먼저 확인해 중복 지급을 막는다.
    private void awardPointIfReviewerJustCompleted(Team team, TeamMember reviewer, List<TeamMember> activeMembers) {
        long expectedReviewsByReviewer = activeMembers.size() - 1L;
        if (expectedReviewsByReviewer <= 0) {
            return;
        }

        // 이탈한 팀원에게 과거에 써둔 리뷰까지 세면 분모(activeMembers)는 줄었는데 분자는 그대로라
        // 실제로는 아직 다 안 썼는데도 "완료"로 잘못 판정될 수 있어, 대상(reviewee)이 현재도
        // 활성 상태인 리뷰만 센다.
        long writtenByReviewer = reviewRepository.countByTeam_TeamIdAndReviewer_TeamMemberIdAndReviewee_Status(
                team.getTeamId(), reviewer.getTeamMemberId(), TeamMemberStatus.ACTIVE);
        if (writtenByReviewer < expectedReviewsByReviewer) {
            return;
        }
        if (collaborationPointService.hasAwarded(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN)) {
            return;
        }
        collaborationPointService.awardPoint(reviewer.getMember(), team, CollaborationPointReason.REVIEW_WRITTEN);
    }

    // 활성 팀원 전원이 서로(자기 자신 제외)를 리뷰했으면 팀을 COMPLETED로 전이시킨다. 활성
    // 팀원이 나가서 1명 이하로 줄면(필요 리뷰 쌍이 0건) 더 이상 쓸 리뷰가 없으므로 그 자체로
    // 완료 처리한다 — writeReview 경로에서는 리뷰어/대상이 모두 활성 상태여야 하므로 이 분기가
    // 절대 실행되지 않고(활성 팀원 2명 미만일 수 없음), recheckAfterMemberLeft에서 팀이 1명
    // 이하로 줄었을 때만 실행된다.
    private void completeReviewIfAllDone(Team team, List<TeamMember> activeMembers) {
        long expectedPairs = (long) activeMembers.size() * (activeMembers.size() - 1);
        if (expectedPairs == 0) {
            chatbotOrchestrationService.completeReview(team);
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
