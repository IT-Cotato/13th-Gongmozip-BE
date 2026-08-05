package org.cotato.gongmozip.domains.chat.enums;

public enum MessageType {
    TEXT,
    SYSTEM_NOTICE,
    LEADER_NOMINATION_CARD, // 팀장 여부 투표 카드 (AI 2명 추천 포함)
    LEADER_VOTE_CARD,
    LEADER_RESULT_CARD, // 팀장 확정 결과 카드 (아바타/이름 노출, 프로필 이동용)
    CONTEST_RECOMMEND_CARD,
    CONTEST_VOTE_CARD,
    PROGRESS_CHECK_CARD,
    SUBMISSION_CHECK_CARD
}
