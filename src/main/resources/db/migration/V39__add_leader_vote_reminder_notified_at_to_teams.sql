-- 팀장 투표 마감 30분 전 리마인더 중복 발송 방지 컬럼
-- 설계 근거: docs/decisions/02-leader-election.md

ALTER TABLE teams ADD COLUMN leader_vote_reminder_notified_at TIMESTAMP NULL;
