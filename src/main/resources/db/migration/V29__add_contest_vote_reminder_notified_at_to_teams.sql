-- 공모전 투표 마감 10분 전 리마인더(Figma "5.1.3.3 팀 공모전 투표하기") 중복 발송 방지 컬럼
-- 설계 근거: docs/decisions/04-contest-voting.md

ALTER TABLE teams ADD COLUMN contest_vote_reminder_notified_at TIMESTAMP NULL;
