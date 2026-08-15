package org.cotato.gongmozip.domains.team.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.team.entity.Team;
import org.cotato.gongmozip.domains.team.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByStatusAndContestCandidateDeadlineAtLessThanEqual(TeamStatus status, LocalDateTime now);

    // 팀장 선출 동률 카드를 두 경로(재투표 요청, AI 추천 수락)가 동시에 소비하려는 경쟁을
    // 직렬화한다 — LeaderElectionService.requestRevote/acceptAiRecommendation 참고.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT team FROM Team team WHERE team.teamId = :teamId")
    Optional<Team> findByIdWithLock(@Param("teamId") Long teamId);

    List<Team> findByStatusAndProgressCheckAtLessThanEqualAndProgressCheckNotifiedAtIsNull(
            TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndSubmissionCheckAtLessThanEqualAndSubmissionCheckNotifiedAtIsNull(
            TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndCreatedAtLessThanEqual(TeamStatus status, LocalDateTime cutoff);

    List<Team> findByStatusAndLeaderCandidacyDeadlineAtLessThanEqual(TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndLeaderVoteDeadlineAtLessThanEqual(TeamStatus status, LocalDateTime now);

    List<Team> findByStatusAndContestCandidateDeadlineAtLessThanEqualAndContestVoteReminderNotifiedAtIsNull(
            TeamStatus status, LocalDateTime reminderThreshold);

    List<Team> findByStatusAndSubmissionCheckReminderAtLessThanEqual(TeamStatus status, LocalDateTime now);
}
