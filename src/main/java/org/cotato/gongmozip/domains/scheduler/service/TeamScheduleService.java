package org.cotato.gongmozip.domains.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시간 기준으로 트리거되는 팀 이벤트(공모전 투표 마감, 중간점검, 제출확인)의 실제 로직.
 * cron 트리거 자체는 {@code TeamSchedulerJobs}가 담당하고, 이 서비스는 단위 테스트가
 * 가능하도록 순수 비즈니스 로직만 담는다 (docs/decisions/07-scheduler.md).
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

    /** 공모전 후보/투표 마감이 지났는데도 CONTEST_SELECTING인 팀을 강제로 확정시킨다. */
    @Transactional
    public void resolveDueContestVotingDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        List<Team> dueTeams = teamRepository.findByStatusAndContestCandidateDeadlineAtLessThanEqual(
                TeamStatus.CONTEST_SELECTING, now);
        for (Team team : dueTeams) {
            contestVotingService.resolveDeadlineIfDue(team.getTeamId());
        }
    }

    /** 중간점검 시각이 지난 IN_PROGRESS 팀에게 진행률 체크 카드를 발행한다 (1회만). */
    @Transactional
    public void sendDueProgressChecks() {
        LocalDateTime now = LocalDateTime.now();
        List<Team> dueTeams =
                teamRepository.findByStatusAndProgressCheckAtLessThanEqualAndProgressCheckNotifiedAtIsNull(
                        TeamStatus.IN_PROGRESS, now);
        for (Team team : dueTeams) {
            team.markProgressCheckNotified(now);
            chatService.postChatbotCardMessage(team, MessageType.PROGRESS_CHECK_CARD, PROGRESS_CHECK_MESSAGE, null);
        }
    }

    /** 제출확인 시각이 지난 IN_PROGRESS 팀에게 제출 여부 확인 카드를 발행한다 (1회만). */
    @Transactional
    public void sendDueSubmissionChecks() {
        LocalDateTime now = LocalDateTime.now();
        List<Team> dueTeams =
                teamRepository.findByStatusAndSubmissionCheckAtLessThanEqualAndSubmissionCheckNotifiedAtIsNull(
                        TeamStatus.IN_PROGRESS, now);
        for (Team team : dueTeams) {
            team.markSubmissionCheckNotified(now);
            chatService.postChatbotCardMessage(team, MessageType.SUBMISSION_CHECK_CARD, SUBMISSION_CHECK_MESSAGE, null);
        }
    }
}
