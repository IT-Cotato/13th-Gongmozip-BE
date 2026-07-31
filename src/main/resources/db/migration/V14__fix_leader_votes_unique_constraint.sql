-- 팀장 투표는 후보를 1명만 선택하는 단일 선택이므로, 원래 제약(team_id, voter_team_member_id,
-- candidate_team_member_id, round)은 같은 라운드에 한 명이 여러 후보에게 투표할 수 있게 되는
-- 버그였다. 라운드당 1인 1표만 가능하도록 수정한다.
-- (docs/decisions/02-leader-election.md 참고)

-- 옛 unique 인덱스(team_id가 leftmost 컬럼)가 team_id FK를 떠받치고 있어서, 새 인덱스가
-- 먼저 있어야 MySQL이 옛 인덱스 삭제를 허용한다 (순서를 바꾸면 에러 1553 발생).
ALTER TABLE leader_votes
    ADD CONSTRAINT uq_leader_votes_team_voter_round UNIQUE (team_id, voter_team_member_id, round);

ALTER TABLE leader_votes DROP CONSTRAINT uq_leader_votes_team_voter_candidate_round;
