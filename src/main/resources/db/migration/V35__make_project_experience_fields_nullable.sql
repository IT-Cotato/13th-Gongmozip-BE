-- V35: project_experiences 테이블의 선택 입력 필드 nullable 전환
ALTER TABLE project_experiences MODIFY COLUMN description TEXT NULL;
ALTER TABLE project_experiences MODIFY COLUMN role VARCHAR(200) NULL;
ALTER TABLE project_experiences MODIFY COLUMN started_at DATE NULL;
ALTER TABLE project_experiences MODIFY COLUMN tech_stacks TEXT NULL;
