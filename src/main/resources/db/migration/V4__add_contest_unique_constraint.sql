-- contest 테이블에 (title, apply_end_at) 유니크 제약조건 추가
ALTER TABLE contest ADD CONSTRAINT uq_contest_title_apply_end_at UNIQUE (title, apply_end_at);
