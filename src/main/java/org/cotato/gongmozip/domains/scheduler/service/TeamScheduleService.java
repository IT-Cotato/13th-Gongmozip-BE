package org.cotato.gongmozip.domains.scheduler.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.chat.enums.MessageType;
import org.cotato.gongmozip.domains.chat.service.ChatService;
import org.cotato.gongmozip.domains.chatbot.service.ChatbotOrchestrationService;
import org.cotato.gongmozip.domains.contest.service.ContestVotingService;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.domains.team.exception.TeamException;
import org.cotato.gongmozip.domains.team.exception.codes.TeamErrorCode;
import org.cotato.gongmozip.domains.team.repository.TeamRepository;
import org.cotato.gongmozip.domains.team.service.LeaderElectionService;
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

    private static final String PROGRESS_CHECK_MESSAGE = "공모전 제출일까지 벌써 절반 왔어요!\n현재까지의 진행률을 팀 내에서 체크해보는 시간을 가져보세요!";
    private static final String SUBMISSION_CHECK_MESSAGE = "공모전 마감일 하루 전입니다. 공모전 제출을 완료했다면 '진행 완료'를, 완료하지 못했다면 '미완료'를 "
            + "선택해주세요. 해당 버튼은 팀장님만 선택할 수 있습니다. 팀장님이 '진행 완료'를 선택하면 본 공모전 "
            + "프로젝트가 종료되며, 팀원 리뷰 단계로 이동합니다.";
    private static final String SUBMISSION_CHECK_REMINDER_MESSAGE = "프로젝트가 진행완료되었으면, 진행완료 버튼을 눌러주세요.";
    // "진행 완료"로 응답하지 않으면 이 간격으로 계속 재알림한다(docs/decisions/07-scheduler.md).
    private static final int SUBMISSION_CHECK_REMINDER_HOURS = 2;
    private static final String CONTEST_VOTE_REMINDER_MESSAGE = "공모전 투표 완료하셨나요? 투표마감까지 10분 남았어요!";
    // 공모전 후보/투표 마감 몇 분 전에 리마인더를 보낼지 (docs/decisions/04-contest-voting.md).
    private static final int CONTEST_VOTE_REMINDER_MINUTES_BEFORE_DEADLINE = 10;
    // 인사 유도 시작(=팀 생성) 후 이 시간 안에 전원이 인사를 마치지 않으면 강제로 다음 단계로 넘긴다
    // (기능명세서 5.1.3.1 E1).
    private static final int GREETING_TIMEOUT_HOURS = 2;

    private final TeamRepository teamRepository;
    private final ContestVotingService contestVotingService;
    private final ChatbotOrchestrationService chatbotOrchestrationService;
    private final ChatService chatService;
    private final LeaderElectionService leaderElectionService;

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

    /**
     * 공모전 후보/투표 마감까지 {@value #CONTEST_VOTE_REMINDER_MINUTES_BEFORE_DEADLINE}분 이내로
     * 남았는데 아직 리마인더를 안 보낸 CONTEST_SELECTING 팀 id 목록을 조회한다. 마감이 이미
     * 지난 팀은 리마인더 카드가 "지금 투표하면 반영된다"는 잘못된 인상을 줄 수 있어(마감 확정
     * 스케줄러가 아직 안 돌았을 뿐) 여기서 걸러낸다 — 최종 방어선은 {@code submitVote}의
     * 마감 시각 검증이지만, 애초에 마감 지난 팀에게 리마인더를 보내지 않는 편이 낫다.
     */
    public List<Long> findDueContestVoteReminderTeamIds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderThreshold = now.plusMinutes(CONTEST_VOTE_REMINDER_MINUTES_BEFORE_DEADLINE);
        return teamRepository
                .findByStatusAndContestCandidateDeadlineAtLessThanEqualAndContestVoteReminderNotifiedAtIsNull(
                        TeamStatus.CONTEST_SELECTING, reminderThreshold)
                .stream()
                .filter(team -> team.getContestCandidateDeadlineAt() != null
                        && team.getContestCandidateDeadlineAt().isAfter(now))
                .map(Team::getTeamId)
                .toList();
    }

    /**
     * 한 팀에게 공모전 투표 마감 리마인더 카드를 발행한다(1회만, 팀 단위 트랜잭션). 대상 id
     * 조회와 실제 발송 사이에 시간이 흐를 수 있으므로, 발송 직전에 마감 시각이 여전히 유효한
     * 리마인더 구간(now ~ now+10분) 안인지 다시 검증한다.
     */
    @Transactional
    public void sendContestVoteReminderForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = team.getContestCandidateDeadlineAt();
        if (team.getStatus() != TeamStatus.CONTEST_SELECTING
                || team.getContestVoteReminderNotifiedAt() != null
                || deadline == null
                || !deadline.isAfter(now)
                || deadline.isAfter(now.plusMinutes(CONTEST_VOTE_REMINDER_MINUTES_BEFORE_DEADLINE))) {
            return;
        }
        team.markContestVoteReminderNotified(now);
        chatService.postChatbotCardMessage(
                team, MessageType.CONTEST_VOTE_REMINDER_CARD, CONTEST_VOTE_REMINDER_MESSAGE, null);
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

    /** 한 팀에게 중간점검 진행률 체크 메시지를 발행한다(1회만, 팀 단위 트랜잭션). */
    @Transactional
    public void sendProgressCheckForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.IN_PROGRESS || team.getProgressCheckNotifiedAt() != null) {
            return;
        }
        team.markProgressCheckNotified(LocalDateTime.now());
        chatService.postChatbotMessage(team, PROGRESS_CHECK_MESSAGE);
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

    /**
     * 한 팀에게 제출 여부 확인 카드를 최초 발행한다(1회만, 팀 단위 트랜잭션). "진행 완료"로
     * 응답하지 않으면 {@link #findDueSubmissionCheckReminderTeamIds}가 이어서 재알림한다.
     */
    @Transactional
    public void sendSubmissionCheckForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        if (team.getStatus() != TeamStatus.IN_PROGRESS || team.getSubmissionCheckNotifiedAt() != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        team.markSubmissionCheckNotified(now);
        team.scheduleSubmissionCheckReminder(now.plusHours(SUBMISSION_CHECK_REMINDER_HOURS));
        chatService.postChatbotCardMessage(team, MessageType.SUBMISSION_CHECK_CARD, SUBMISSION_CHECK_MESSAGE, null);
    }

    /**
     * "진행 완료"로 아직 응답하지 않아 재알림 시각이 지난 IN_PROGRESS 팀 id 목록을 조회한다
     * (Figma "제출 여부 미진행시"). 최초 발송, 팀장의 "미완료" 응답, 이 재알림 발송 자체가 모두
     * {@code submissionCheckReminderAt}을 now+2시간으로 계속 미루므로, "진행 완료"로 팀 상태가
     * SUBMITTED로 바뀌기 전까지 2시간 간격으로 반복된다.
     */
    public List<Long> findDueSubmissionCheckReminderTeamIds() {
        return teamRepository
                .findByStatusAndSubmissionCheckReminderAtLessThanEqual(TeamStatus.IN_PROGRESS, LocalDateTime.now())
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /**
     * 한 팀에게 제출 여부 확인 재알림 카드를 발행하고 다음 재알림 시각을 다시 미룬다(팀 단위
     * 트랜잭션). 대상 id 조회 이후 팀장이 "미완료"로 다시 응답해 재알림 시각이 미래로 갱신됐을
     * 수 있으므로, 발송 직전에 재알림 시각이 아직 지났는지 다시 검증한다.
     */
    @Transactional
    public void sendSubmissionCheckReminderForTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        if (team.getStatus() != TeamStatus.IN_PROGRESS
                || team.getSubmissionCheckReminderAt() == null
                || team.getSubmissionCheckReminderAt().isAfter(now)) {
            return;
        }
        team.scheduleSubmissionCheckReminder(now.plusHours(SUBMISSION_CHECK_REMINDER_HOURS));
        chatService.postChatbotCardMessage(
                team, MessageType.SUBMISSION_CHECK_CARD, SUBMISSION_CHECK_REMINDER_MESSAGE, null);
    }

    /** 인사 유도 시작(팀 생성) 후 타임아웃이 지났는데도 여전히 GREETING인 팀 id 목록을 조회한다. */
    public List<Long> findDueGreetingTimeoutTeamIds() {
        return teamRepository
                .findByStatusAndCreatedAtLessThanEqual(
                        TeamStatus.GREETING, LocalDateTime.now().minusHours(GREETING_TIMEOUT_HOURS))
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /** 한 팀의 인사 유도 단계를 강제로 다음 단계로 넘긴다(팀 단위 트랜잭션). */
    @Transactional
    public void forceAdvanceGreetingForTeam(Long teamId) {
        chatbotOrchestrationService.forceAdvanceGreetingIfDue(teamId);
    }

    /** 팀장 후보 등록(팀장 여부 투표) 마감이 지났는데도 LEADER_SELECTING인 팀 id 목록을 조회한다. */
    public List<Long> findDueLeaderCandidacyDeadlineTeamIds() {
        return teamRepository
                .findByStatusAndLeaderCandidacyDeadlineAtLessThanEqual(TeamStatus.LEADER_SELECTING, LocalDateTime.now())
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /** 한 팀의 팀장 후보 등록 마감을 강제로 확정 처리한다(팀 단위 트랜잭션). */
    @Transactional
    public void resolveLeaderCandidacyDeadlineForTeam(Long teamId) {
        leaderElectionService.resolveCandidacyDeadlineIfDue(teamId);
    }

    /** 팀장 투표 마감이 지났는데도 LEADER_SELECTING인 팀 id 목록을 조회한다. */
    public List<Long> findDueLeaderVoteDeadlineTeamIds() {
        return teamRepository
                .findByStatusAndLeaderVoteDeadlineAtLessThanEqual(TeamStatus.LEADER_SELECTING, LocalDateTime.now())
                .stream()
                .map(Team::getTeamId)
                .toList();
    }

    /** 한 팀의 팀장 투표 마감을 강제로 확정 처리한다(팀 단위 트랜잭션). */
    @Transactional
    public void resolveLeaderVoteDeadlineForTeam(Long teamId) {
        leaderElectionService.resolveVoteDeadlineIfDue(teamId);
    }
}
