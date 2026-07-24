-- contest 테이블에 (title, apply_end_at) 유니크 제약조건 추가
-- 이전 V3 버전에 constraint가 포함된 경우를 위한 멱등 처리: 있으면 DROP 후 재추가
SET @n = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contest'
      AND INDEX_NAME = 'uq_contest_title_apply_end_at'
);
SET @drop_sql = IF(@n > 0,
    'ALTER TABLE contest DROP INDEX uq_contest_title_apply_end_at',
    'SELECT 1');
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

ALTER TABLE contest
    ADD CONSTRAINT uq_contest_title_apply_end_at UNIQUE (title, apply_end_at);
