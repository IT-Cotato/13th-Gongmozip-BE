-- contest 테이블에 (title, apply_end_at) 유니크 제약조건 추가
-- 멱등 처리: 이미 존재하는 경우 DROP 후 재추가
ALTER TABLE contest DROP INDEX IF EXISTS uq_contest_title_apply_end_at;
ALTER TABLE contest ADD CONSTRAINT uq_contest_title_apply_end_at UNIQUE (title, apply_end_at);
