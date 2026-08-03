-- 신청 시 고정 점수구간을 저장하지 않고, 14시 배치에서 백분위/병합 결과를 확정한다.
ALTER TABLE matching_applications
    MODIFY COLUMN skill_group INT NULL;

-- 기존 skill_group 물리 컬럼은 유효 풀의 순번(pool ordinal)으로 사용한다.
ALTER TABLE matching_batches
    ADD COLUMN grouping_mode VARCHAR(30) NOT NULL DEFAULT 'QUARTILE';

ALTER TABLE matching_batches
    ADD COLUMN source_quartile_from INT NOT NULL DEFAULT 1;

ALTER TABLE matching_batches
    ADD COLUMN source_quartile_to INT NOT NULL DEFAULT 4;

-- 하나의 결과 그룹이 3인인지 4인인지 결과 자체에 남긴다.
ALTER TABLE matching_groups
    ADD COLUMN team_size INT NOT NULL DEFAULT 4;

ALTER TABLE matching_groups
    ADD CONSTRAINT chk_matching_groups_team_size
        CHECK (team_size IN (3, 4));
