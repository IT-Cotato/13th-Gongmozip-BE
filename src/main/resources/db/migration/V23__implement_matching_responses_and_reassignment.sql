-- V23: PROPOSED 매칭 결과에 대한 수락·패스·마감 처리와 실제 Team 생성,
--      그리고 그룹이 성사되지 않았을 때의 자동 재매칭 이력을 저장한다.

-- -----------------------------------------------------------------------------
-- 1. matching_groups: 그룹 전체의 응답 마감 및 최종 처리 결과
-- -----------------------------------------------------------------------------

-- 신청일 다음 날 12시. 이 시각부터 신규 수락·패스를 받지 않고 마감 작업 대상이 된다.
ALTER TABLE matching_groups
    ADD COLUMN response_deadline_at TIMESTAMP NULL;

-- ACCEPTED 인원이 3명 또는 4명이 되어 실제 Team 생성까지 완료된 시각이다.
ALTER TABLE matching_groups
    ADD COLUMN confirmed_at TIMESTAMP NULL;

-- 수동 패스로 유효 인원이 3명 미만이 되어 그룹이 CANCELED된 시각이다.
ALTER TABLE matching_groups
    ADD COLUMN canceled_at TIMESTAMP NULL;

-- 12시 마감 처리 결과 수락 인원이 3명 미만이어서 그룹이 EXPIRED된 시각이다.
ALTER TABLE matching_groups
    ADD COLUMN expired_at TIMESTAMP NULL;

-- 최초 제안 인원(team_size)과 별개인 실제 확정 인원이다.
-- 예: 4인 제안에서 1명이 패스하고 나머지 3명이 수락하면 team_size=4, confirmed_team_size=3이다.
ALTER TABLE matching_groups
    ADD COLUMN confirmed_team_size INT NULL;

-- 모든 유효 인원이 수락했을 때 생성한 실제 Team을 연결한다.
-- 아직 응답 대기 중이거나 취소·만료된 그룹은 NULL을 유지한다.
ALTER TABLE matching_groups
    ADD COLUMN team_id BIGINT NULL;

-- 실제 팀은 현재 정책상 3명 또는 4명으로만 확정할 수 있다.
ALTER TABLE matching_groups
    ADD CONSTRAINT chk_matching_groups_confirmed_team_size
        CHECK (confirmed_team_size IS NULL OR confirmed_team_size IN (3, 4));

-- 연결된 Team이 실제 teams 테이블에 존재하도록 참조 무결성을 보장한다.
ALTER TABLE matching_groups
    ADD CONSTRAINT fk_matching_groups_team
        FOREIGN KEY (team_id) REFERENCES teams (team_id);

-- 하나의 Team이 여러 MatchingGroup의 결과로 중복 연결되는 것을 막는다.
-- MatchingGroup 한 행에는 team_id가 하나뿐이므로 그룹당 최종 Team도 하나만 보관된다.
ALTER TABLE matching_groups
    ADD CONSTRAINT uq_matching_groups_team UNIQUE (team_id);

-- 12시 작업이 PROPOSED이면서 마감 시각이 지난 그룹만 빠르게 찾도록 한다.
CREATE INDEX idx_matching_groups_status_deadline
    ON matching_groups (status, response_deadline_at);

-- -----------------------------------------------------------------------------
-- 2. matching_group_members: 각 그룹원의 응답이 제출된 경로와 패널티
-- -----------------------------------------------------------------------------

-- 응답 처리 주체를 구분한다: USER=직접 수락/패스, DEADLINE_JOB=12시 자동 만료.
ALTER TABLE matching_group_members
    ADD COLUMN response_source VARCHAR(30) NULL;

-- 해당 응답에서 계산한 정책상 누적 패널티 값(3/5/7/9/11)을 스냅샷으로 기록한다.
-- 실제 포인트 증감 원장은 collaboration_point_histories.delta이며, 이 값은 같은 패스 요청이
-- 재시도됐을 때 최초 계산값을 반환하면서 포인트를 다시 차감하지 않기 위해 사용한다.
-- ACCEPTED 또는 아직 PENDING인 그룹원은 NULL이다.
ALTER TABLE matching_group_members
    ADD COLUMN pass_penalty INT NULL;

-- -----------------------------------------------------------------------------
-- 3. matching_applications: 자동 재매칭 신청과 원본 신청의 연결
-- -----------------------------------------------------------------------------

-- 자동 생성된 재매칭 신청이 어느 이전 신청에서 파생됐는지 가리킨다.
-- 일반 직접 신청은 NULL이며, 연속 재매칭은 이 자기참조 관계를 체인으로 추적할 수 있다.
ALTER TABLE matching_applications
    ADD COLUMN source_application_id BIGINT NULL;

-- 연속 자동 재매칭 횟수다. 일반 신청은 0, 첫 재매칭은 1부터 시작한다.
ALTER TABLE matching_applications
    ADD COLUMN reassignment_count INT NOT NULL DEFAULT 0;

-- 재매칭 사유를 구분한다: GROUP_MEMBER_PASSED 또는 RESPONSE_DEADLINE_EXPIRED.
ALTER TABLE matching_applications
    ADD COLUMN reassignment_reason VARCHAR(50) NULL;

-- 원본으로 기록된 신청이 실제 matching_applications 행인지 보장한다.
ALTER TABLE matching_applications
    ADD CONSTRAINT fk_matching_applications_source
        FOREIGN KEY (source_application_id) REFERENCES matching_applications (matching_application_id);

-- 원본 신청을 기준으로 파생된 재매칭 신청 이력을 빠르게 조회하도록 한다.
CREATE INDEX idx_matching_applications_source
    ON matching_applications (source_application_id);

-- -----------------------------------------------------------------------------
-- 4. matching_batches: 파생 가능한 결과 공개 시각 제거
-- -----------------------------------------------------------------------------

-- 결과 공개 시각은 application_date와 YML의 result-publish-time 정책으로 계산한다.
-- 배치별 공개 시각을 별도로 운영하지 않으므로 파생값과 그 전용 인덱스를 제거한다.
DROP INDEX idx_matching_batches_status_publish ON matching_batches;

ALTER TABLE matching_batches
    DROP COLUMN published_at;
