-- Phase 7: 스케줄러(중간점검/제출확인/공모전 투표 마감) 관련 컬럼
-- 설계 근거: docs/decisions/07-scheduler.md, 04-contest-voting.md

ALTER TABLE teams ADD COLUMN contest_candidate_deadline_at TIMESTAMP NULL;
ALTER TABLE teams ADD COLUMN progress_check_notified_at TIMESTAMP NULL;
ALTER TABLE teams ADD COLUMN progress_check_responded_at TIMESTAMP NULL;
ALTER TABLE teams ADD COLUMN submission_check_notified_at TIMESTAMP NULL;
