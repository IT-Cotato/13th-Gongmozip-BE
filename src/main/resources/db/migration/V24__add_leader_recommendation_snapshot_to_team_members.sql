-- V24: 팀장 선출/추천 알고리즘(docs/decisions/02-leader-election.md)에 필요한 팀 생성 시점
--      스냅샷 3종을 team_members에 추가한다. 매칭 신청(matching_applications)에 이미 저장돼
--      있던 값을 팀 생성 시점에 그대로 복사해온다.

ALTER TABLE team_members
    ADD COLUMN leader_preference VARCHAR(30) NOT NULL DEFAULT 'NEUTRAL';

ALTER TABLE team_members
    ADD COLUMN extroversion_type VARCHAR(1) NOT NULL DEFAULT 'A';

ALTER TABLE team_members
    ADD COLUMN extroversion_score DECIMAL(5, 2) NOT NULL DEFAULT 3.00;
