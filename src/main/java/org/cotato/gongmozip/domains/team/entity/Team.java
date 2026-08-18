package org.cotato.gongmozip.domains.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.team.enums.LeaderSelectionMode;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "teams")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id", nullable = false, updatable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TeamStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_category", nullable = false, length = 50)
    private InterestCategory preferredCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "leader_selection_mode", nullable = false, length = 30)
    private LeaderSelectionMode leaderSelectionMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @Column(name = "chatbot_enabled", nullable = false)
    private boolean chatbotEnabled;

    @Column(name = "progress_check_at")
    private LocalDateTime progressCheckAt;

    @Column(name = "submission_check_at")
    private LocalDateTime submissionCheckAt;

    // 공모전 후보/투표 마감 시각. CONTEST_SELECTING 진입 시 세팅되며, 스케줄러가 이 시각이
    // 지났는데도 팀이 CONTEST_SELECTING이면 강제로 결과를 확정한다.
    @Column(name = "contest_candidate_deadline_at")
    private LocalDateTime contestCandidateDeadlineAt;

    // 팀장 "후보 등록"(팀장 여부 투표) 마감 시각. LEADER_SELECTING 진입 시 세팅되며, 스케줄러가
    // 이 시각이 지났는데도 후보 등록이 안 끝났으면 응답 안 한 사람을 "안 할래요"로 간주하고
    // 강제로 다음 단계로 넘긴다 (docs/decisions/02-leader-election.md 참고). AUTO_ASSIGNED는
    // 대기 없이 즉시 확정되므로 세팅되지 않는다.
    @Column(name = "leader_candidacy_deadline_at")
    private LocalDateTime leaderCandidacyDeadlineAt;

    // 팀장 "투표" 마감 시각. 후보 등록이 끝나 투표 카드가 처음 발행될 때 세팅되고, 동률로 재투표
    // 카드가 다시 발행될 때마다 새로 세팅된다(라운드마다 독립된 마감, 2026-08-15 갱신 — 후보
    // 등록 마감과 분리하면서 결정, docs/decisions/02-leader-election.md 참고). 스케줄러가 이
    // 시각이 지났는데도 팀이 LEADER_SELECTING이면 그 라운드를 강제로 확정한다.
    @Column(name = "leader_vote_deadline_at")
    private LocalDateTime leaderVoteDeadlineAt;

    // 중간점검/제출확인 알림이 이미 발행되었는지 추적하는 멱등성 플래그(스케줄러 중복 발행 방지).
    // 중간점검은 일반 텍스트 메시지, 제출확인은 카드로 발행되며 이 필드는 둘 다에 쓰인다.
    @Column(name = "progress_check_notified_at")
    private LocalDateTime progressCheckNotifiedAt;

    @Column(name = "submission_check_notified_at")
    private LocalDateTime submissionCheckNotifiedAt;

    // 제출 여부 확인에 아직 "진행 완료"로 응답하지 않았을 때 다음 재알림을 보낼 시각(Figma
    // "제출 여부 미진행시"). 최초 발송, 팀장의 "미완료" 응답, 재알림 발송 시점마다 now+2시간으로
    // 계속 미뤄지며, "진행 완료"로 상태가 SUBMITTED로 바뀌면 더 이상 조회 대상이 아니게 되어
    // 자연히 멈춘다(명시적으로 null로 지우지 않음 — leaderCandidacyDeadlineAt과 동일한 이유).
    @Column(name = "submission_check_reminder_at")
    private LocalDateTime submissionCheckReminderAt;

    // 공모전 투표 마감 10분 전 리마인더가 이미 발행되었는지 추적하는 멱등성 플래그(스케줄러
    // 중복 발행 방지, docs/decisions/04-contest-voting.md).
    @Column(name = "contest_vote_reminder_notified_at")
    private LocalDateTime contestVoteReminderNotifiedAt;

    @Column(name = "submitted", nullable = false)
    private boolean submitted;

    @Column(name = "contest_decided_at")
    private LocalDateTime contestDecidedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // GREETING 상태 전이 동시 실행 방지용 낙관적 잠금(이슈 #62). 스케줄러(forceAdvanceGreetingIfDue)와
    // 팀원 메시지 트리거(recordGreetingAndAdvance)가 동시에 같은 팀을 LEADER_SELECTING으로 전이시키려
    // 하면 나중에 커밋하는 쪽이 OptimisticLockingFailureException을 받아 재시도/스킵할 수 있다.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public void setChatbotEnabled(boolean chatbotEnabled) {
        this.chatbotEnabled = chatbotEnabled;
    }

    public void advanceStatus(TeamStatus status) {
        this.status = status;
    }

    public void assignContest(Contest contest) {
        this.contest = contest;
        this.contestDecidedAt = LocalDateTime.now();
    }

    public void scheduleCheckpoints(LocalDateTime progressCheckAt, LocalDateTime submissionCheckAt) {
        this.progressCheckAt = progressCheckAt;
        this.submissionCheckAt = submissionCheckAt;
    }

    public void scheduleContestCandidateDeadline(LocalDateTime contestCandidateDeadlineAt) {
        this.contestCandidateDeadlineAt = contestCandidateDeadlineAt;
    }

    public void scheduleLeaderCandidacyDeadline(LocalDateTime leaderCandidacyDeadlineAt) {
        this.leaderCandidacyDeadlineAt = leaderCandidacyDeadlineAt;
    }

    public void scheduleLeaderVoteDeadline(LocalDateTime leaderVoteDeadlineAt) {
        this.leaderVoteDeadlineAt = leaderVoteDeadlineAt;
    }

    public void markProgressCheckNotified(LocalDateTime notifiedAt) {
        this.progressCheckNotifiedAt = notifiedAt;
    }

    public void markContestVoteReminderNotified(LocalDateTime notifiedAt) {
        this.contestVoteReminderNotifiedAt = notifiedAt;
    }

    public void markSubmissionCheckNotified(LocalDateTime notifiedAt) {
        this.submissionCheckNotifiedAt = notifiedAt;
    }

    public void scheduleSubmissionCheckReminder(LocalDateTime reminderAt) {
        this.submissionCheckReminderAt = reminderAt;
    }

    public void markSubmitted() {
        this.submitted = true;
        this.status = TeamStatus.SUBMITTED;
        this.completedAt = LocalDateTime.now();
    }
}
