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

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "progress_check_at")
    private LocalDateTime progressCheckAt;

    @Column(name = "submission_check_at")
    private LocalDateTime submissionCheckAt;

    // 공모전 후보/투표 마감 시각. CONTEST_SELECTING 진입 시 세팅되며, 스케줄러가 이 시각이
    // 지났는데도 팀이 CONTEST_SELECTING이면 강제로 결과를 확정한다.
    @Column(name = "contest_candidate_deadline_at")
    private LocalDateTime contestCandidateDeadlineAt;

    // 팀장 여부 투표/팀장 투표 마감 시각. LEADER_SELECTING 진입 시(팀장 여부 투표 카드 또는
    // 후보 투표 카드 발행 시점) 세팅되며, 스케줄러가 이 시각이 지났는데도 팀이 LEADER_SELECTING
    // 이면 강제로 결과를 확정한다 (docs/decisions/02-leader-election.md 참고). AUTO_ASSIGNED는
    // 대기 없이 즉시 확정되므로 세팅되지 않는다.
    @Column(name = "leader_selection_deadline_at")
    private LocalDateTime leaderSelectionDeadlineAt;

    // 중간점검/제출확인 카드가 이미 발행되었는지 추적하는 멱등성 플래그(스케줄러 중복 발행 방지).
    @Column(name = "progress_check_notified_at")
    private LocalDateTime progressCheckNotifiedAt;

    @Column(name = "progress_check_responded_at")
    private LocalDateTime progressCheckRespondedAt;

    @Column(name = "submission_check_notified_at")
    private LocalDateTime submissionCheckNotifiedAt;

    @Column(name = "submitted", nullable = false)
    private boolean submitted;

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
    }

    public void scheduleCheckpoints(LocalDateTime progressCheckAt, LocalDateTime submissionCheckAt) {
        this.progressCheckAt = progressCheckAt;
        this.submissionCheckAt = submissionCheckAt;
    }

    public void scheduleContestCandidateDeadline(LocalDateTime contestCandidateDeadlineAt) {
        this.contestCandidateDeadlineAt = contestCandidateDeadlineAt;
    }

    public void scheduleLeaderSelectionDeadline(LocalDateTime leaderSelectionDeadlineAt) {
        this.leaderSelectionDeadlineAt = leaderSelectionDeadlineAt;
    }

    public void markProgressCheckNotified(LocalDateTime notifiedAt) {
        this.progressCheckNotifiedAt = notifiedAt;
    }

    public void recordProgress(int progressPercent, LocalDateTime respondedAt) {
        this.progressPercent = progressPercent;
        this.progressCheckRespondedAt = respondedAt;
    }

    public void markSubmissionCheckNotified(LocalDateTime notifiedAt) {
        this.submissionCheckNotifiedAt = notifiedAt;
    }

    public void markSubmitted() {
        this.submitted = true;
        this.status = TeamStatus.SUBMITTED;
    }
}
