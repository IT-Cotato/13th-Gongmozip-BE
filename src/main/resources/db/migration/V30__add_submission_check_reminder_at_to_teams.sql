-- 제출 여부 확인 미응답/미완료 시 2시간 간격으로 재알림하기 위한 컬럼 (Figma "제출 여부 미진행시")
-- 설계 근거: docs/decisions/07-scheduler.md

ALTER TABLE teams ADD COLUMN submission_check_reminder_at TIMESTAMP NULL;
