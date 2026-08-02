package org.cotato.gongmozip.domains.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시간 기준으로 트리거되는 팀 이벤트(공모전 투표 마감, 중간점검, 제출확인)의 실제 로직.
 * cron 트리거 자체는 {@code TeamSchedulerJobs}가 담당하고, 이 서비스는 단위 테스트가
 * 가능하도록 순수 비즈니스 로직만 담는다 (docs/decisions/07-scheduler.md).
 *
 * <p>대상 팀을 "조회"하는 메서드와 "팀 1개를 처리"하는 메서드를 분리했다 — 대상 팀 전체를 하나의
 * 트랜잭션으로 묶으면 팀이 많아질수록 커넥션을 오래 점유하고, 한 팀 처리 중 예외가 나면 이미
 * 처리된 다른 팀들까지 롤백된다. {@code TeamSchedulerJobs}가 조회 결과를 순회하며 팀마다
 * 별도 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamScheduleService {

    private static final String PROGRESS_CHECK_MESSAGE =
            "팀원들과 회의를 잘 진행하고 있나요? 현재 진행률을 체크해주세요 :) 진행률 체크는 팀장님만 할 수 있습니다.";
    private static final String SUBMISSION_CHECK_MESSAGE = "공모전 마감일 하루 전입니다. 공모전 제출을 완료했다면 '진행 완료'를, 완료하지 못했다면 '미완료'를 "
            + "선택해주세요. 해당 버튼은 팀장님만 선택할 수 있습니다. 팀장님이 '진행 완료'를 선택하면 본 공모전 "
            + "프로젝트가 종료되며, 팀원 리뷰 단계로 이동합니다.";

    private final TeamRepository teamRepository;
    private final ContestVotingService contestVotingService;
    private final ChatService chatService;

    /** 공모전 후보/투표 마감이 지났는데도 CONTEST_SELECTING인 팀 id 목록을 조회한다. */
    public List<Long> findDueContestVotingDeadlineTeamIds() {
        return teamRepository
                .findByStatusAndContestCandidateDeadlineAtLessThanEqual(
                        TeamStatus.CONTEST_SELECTING, LocalDateTime.now())
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /** 한 팀의 공모전 투표 마감을 강제로 확정 처리한다(팀 단위 트랜잭션). */
    @Transactional
    public void resolveContestVotingDeadlineForTeam(Long teamId) {
        contestVotingService.resolveDeadlineIfDue(teamId);
    }

    /** 중간점검 시각이 지났는데 아직 알림을 안 보낸 IN_PROGRESS 팀 id 목록을 조회한다. */
    public List<Long> findDueProgressCheckTeamIds() {
        return teamRepository
                .findByStatusAndProgressCheckAtLessThanEqualAndProgressCheckNotifiedAtIsNull(
                        TeamStatus.IN_PROGRESS, LocalDateTime.now())
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /** 한 팀에게 중간점검 진행률 체크 카드를 발행한다(1회만, 팀 단위 트랜잭션). */
    @Transactional
    public void sendProgressCheckForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.IN_PROGRESS || team.getProgressCheckNotifiedAt() != null) {
            return;
        }
        team.markProgressCheckNotified(LocalDateTime.now());
        chatService.postChatbotCardMessage(team, MessageType.PROGRESS_CHECK_CARD, PROGRESS_CHECK_MESSAGE, null);
    }

    /** 제출확인 시각이 지났는데 아직 알림을 안 보낸 IN_PROGRESS 팀 id 목록을 조회한다. */
    public List<Long> findDueSubmissionCheckTeamIds() {
        return teamRepository
                .findByStatusAndSubmissionCheckAtLessThanEqualAndSubmissionCheckNotifiedAtIsNull(
                        TeamStatus.IN_PROGRESS, LocalDateTime.now())
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /** 한 팀에게 제출 여부 확인 카드를 발행한다(1회만, 팀 단위 트랜잭션). */
    @Transactional
    public void sendSubmissionCheckForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.IN_PROGRESS || team.getSubmissionCheckNotifiedAt() != null) {
            return;
        }
        team.markSubmissionCheckNotified(LocalDateTime.now());
        chatService.postChatbotCardMessage(team, MessageType.SUBMISSION_CHECK_CARD, SUBMISSION_CHECK_MESSAGE, null);
    }
}
