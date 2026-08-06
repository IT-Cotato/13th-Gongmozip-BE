-- V28: GREETING 상태 전이 동시 실행 방지를 위한 낙관적 잠금 버전 컬럼 (이슈 #62)
-- 스케줄러(forceAdvanceGreetingIfDue)와 팀원 메시지 트리거(recordGreetingAndAdvance)가
-- 동시에 같은 팀의 GREETING 조건을 통과해 advanceToLeaderSelecting이 중복 실행되는 것을 막는다.

ALTER TABLE teams ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
