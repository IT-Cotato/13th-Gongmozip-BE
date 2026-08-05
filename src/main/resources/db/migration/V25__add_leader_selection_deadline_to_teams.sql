-- V25: 팀장 여부 투표/팀장 투표 마감 시각을 저장한다. LEADER_SELECTING 진입 시 세팅되고,
--      스케줄러가 이 시각이 지났는데도 팀이 LEADER_SELECTING이면 강제로 결과를 확정한다
--      (docs/decisions/02-leader-election.md 참고).

ALTER TABLE teams
    ADD COLUMN leader_selection_deadline_at TIMESTAMP NULL;
