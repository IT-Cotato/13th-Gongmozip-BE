-- V17: 프로젝트 경험 AI 평가 테이블 추가 (팀원 매칭용 개인 역량 점수 산출)
CREATE TABLE project_evaluations
(
    project_evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id            BIGINT      NOT NULL,
    score                 INT         NULL,
    feedback              TEXT        NULL,
    status                VARCHAR(30) NOT NULL,
    error_message         TEXT        NULL,
    evaluated_at          TIMESTAMP   NULL,
    created_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_evaluations_project FOREIGN KEY (project_id) REFERENCES project_experiences (project_id) ON DELETE CASCADE,
    CONSTRAINT chk_project_evaluations_score CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    CONSTRAINT uq_project_evaluations_project UNIQUE (project_id)
);
