ALTER TABLE project_experiences ADD COLUMN ai_summary_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CREATED';
ALTER TABLE project_experiences ADD COLUMN ai_summary_generated_at TIMESTAMP NULL;

-- 기존 요약 완료 데이터 보정
UPDATE project_experiences 
SET ai_summary_status = 'COMPLETED', ai_summary_generated_at = NOW() 
WHERE ai_summary IS NOT NULL;
