-- V33: 프로젝트 경험 상세 평가 점수(R, O, F) 및 인젝션 감지 플래그 추가, 프로젝트 카테고리 추가
ALTER TABLE project_experiences ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'CONTEST';

ALTER TABLE project_evaluations ADD COLUMN r_score INT DEFAULT NULL;
ALTER TABLE project_evaluations ADD COLUMN o_score INT DEFAULT NULL;
ALTER TABLE project_evaluations ADD COLUMN f_score INT DEFAULT NULL;
ALTER TABLE project_evaluations ADD COLUMN injection_detected BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE project_evaluations ADD CONSTRAINT chk_project_evaluations_r_score CHECK (r_score IS NULL OR (r_score >= 0 AND r_score <= 5));
ALTER TABLE project_evaluations ADD CONSTRAINT chk_project_evaluations_o_score CHECK (o_score IS NULL OR (o_score >= 0 AND o_score <= 5));
ALTER TABLE project_evaluations ADD CONSTRAINT chk_project_evaluations_f_score CHECK (f_score IS NULL OR (f_score >= 0 AND f_score <= 5));
