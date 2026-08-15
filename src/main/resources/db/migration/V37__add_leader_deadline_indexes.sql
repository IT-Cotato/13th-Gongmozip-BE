-- findDueLeaderCandidacyDeadlineTeamIds/findDueLeaderVoteDeadlineTeamIds가 5분마다
-- status=LEADER_SELECTING AND ...deadline_at <= now로 조회하므로 인덱스가 없으면 매 주기
-- 풀스캔한다(idx_teams_status_submission_check_reminder_at과 동일한 이유, V30 참고).
-- 원래 leader_selection_deadline_at에도 없던 인덱스라 이번에 같이 챙긴다.
CREATE INDEX idx_teams_status_leader_candidacy_deadline_at
    ON teams (status, leader_candidacy_deadline_at);
CREATE INDEX idx_teams_status_leader_vote_deadline_at
    ON teams (status, leader_vote_deadline_at);
