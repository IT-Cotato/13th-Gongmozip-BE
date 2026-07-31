package org.cotato.gongmozip.domains.team.enums;

public enum TeamStatus {
    MATCHED, // 매칭 완료, 인사 유도 전
    GREETING, // 자기소개/인사 진행 중
    LEADER_SELECTING, // 팀장 선출 진행 중 (OPEN_NOMINATION/CANDIDATE_VOTE만 거침)
    LEADER_DECIDED, // 팀장 확정
    CONTEST_SELECTING, // 공모전 후보 모집 중
    CONTEST_VOTING, // 공모전 투표 진행 중
    CONTEST_DECIDED, // 공모전 확정
    IN_PROGRESS, // 공모전 준비 진행 중
    SUBMITTED, // 팀장이 제출 완료 처리
    COMPLETED // 팀원 리뷰까지 종료
}
