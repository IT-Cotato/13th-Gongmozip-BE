package org.cotato.gongmozip.domains.team.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.collaboration.service.CollaborationPointService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.entity.TeamMember;
import org.cotato.gongmozip.domains.team.enums.TeamMemberStatus;
import org.cotato.gongmozip.domains.team.enums.TeamRole;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamMemberRepository;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중간점검 응답 / 공모전 제출 여부 확인. 둘 다 팀장만 응답할 수 있다
 * (docs/decisions/07-scheduler.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamProgressService {

    // 제출 여부 확인에 "진행 완료"로 응답하지 않으면 이 간격으로 계속 재알림한다
    // (GREETING/팀장 선출 타임아웃과 동일한 2시간 간격, docs/decisions/07-scheduler.md).
    private static final int SUBMISSION_CHECK_REMINDER_HOURS = 2;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ChatService chatService;
    private final CollaborationPointService collaborationPointService;

    @Transactional
    public void updateProgress(Long teamId, Long memberId, int progressPercent) {
        Team team = requireTeamInProgress(teamId);
        TeamMember leader = requireLeader(teamId, memberId);

        boolean firstResponse = team.getProgressCheckRespondedAt() == null;
        team.recordProgress(progressPercent, LocalDateTime.now());

        if (firstResponse) {
            collaborationPointService.awardPoint(
                    leader.getMember(), team, CollaborationPointReason.PROGRESS_CHECK_RESPONSE);
            chatService.postSystemMessage(team, "진행률에 응답해주셔서 협업거리가 5m 증가했습니다.");
        }
    }

    @Transactional
    public void submitCompletion(Long teamId, Long memberId, boolean completed) {
        Team team = requireTeamInProgress(teamId);
        TeamMember leader = requireLeader(teamId, memberId);

        if (!completed) {
            team.scheduleSubmissionCheckReminder(LocalDateTime.now().plusHours(SUBMISSION_CHECK_REMINDER_HOURS));
            chatService.postSystemMessage(team, "아직 제출 전이군요. 완료되면 다시 알려주세요!");
            return;
        }

        team.markSubmitted();
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE);
        for (TeamMember member : activeMembers) {
            boolean isLeader = member.getRole() == TeamRole.LEADER;
            collaborationPointService.awardPoint(
                    member.getMember(),
                    team,
                    isLeader
                            ? CollaborationPointReason.PROJECT_COMPLETE_LEADER
                            : CollaborationPointReason.PROJECT_COMPLETE_MEMBER);
        }
        chatService.postSystemMessage(team, leader.getProfile().getNickname() + "님의 프로젝트 완주를 축하드려요!");
    }

    private Team requireTeamInProgress(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.IN_PROGRESS) {
            throw new TeamException(TeamErrorCode.INVALID_TEAM_STATUS);
        }
        return team;
    }

    private TeamMember requireLeader(Long teamId, Long memberId) {
        TeamMember member = teamMemberRepository
                .findByTeam_TeamIdAndMember_MemberId(teamId, memberId)
                .filter(teamMember -> teamMember.getStatus() == TeamMemberStatus.ACTIVE)
                .orElseThrow(() -> new TeamException(TeamErrorCode.NOT_TEAM_MEMBER));
        if (member.getRole() != TeamRole.LEADER) {
            throw new TeamException(TeamErrorCode.NOT_TEAM_LEADER);
        }
        return member;
    }
}
