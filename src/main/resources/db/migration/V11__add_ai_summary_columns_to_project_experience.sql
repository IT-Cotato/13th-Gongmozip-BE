ALTER TABLE project_experiences ADD COLUMN ai_summary_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CREATED';
ALTER TABLE project_experiences ADD COLUMN ai_summary_generated_at TIMESTAMP NULL;

-- 기존 요약 완료 데이터 보정 (생성 시간 정보가 불명확하므로 NULL 유지)
UPDATE project_experiences 
SET ai_summary_status = 'COMPLETED' 
WHERE ai_summary IS NOT NULL;
