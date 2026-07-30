package org.cotato.gongmozip.domains.team.enums;

/**
 * 팀 생성 시점의 팀장 희망 점수 합산으로 결정되는 팀장 선출 경로.
 * 현재는 팀장 희망 점수 데이터가 존재하지 않아 항상 OPEN_NOMINATION으로 고정한다.
 * 자세한 내용은 docs/decisions/02-leader-election.md 참고.
 */
public enum LeaderSelectionMode {
    CANDIDATE_VOTE, // 사전 후보(팀장 희망 점수=1.0) 2명 이상 — 여부투표 없이 바로 팀장 투표
    AUTO_ASSIGNED, // 사전 후보 점수 합 1.0~1.5 — 투표 없이 유일 후보를 바로 팀장으로 확정
    OPEN_NOMINATION // 그 외 — 인사 유도 후 팀장 여부 투표(AI 2명 추천) → 팀장 투표
}
