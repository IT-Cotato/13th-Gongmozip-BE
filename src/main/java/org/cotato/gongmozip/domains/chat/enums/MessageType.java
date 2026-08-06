package org.cotato.gongmozip.domains.chat.enums;

public enum MessageType {
    TEXT,
    SYSTEM_NOTICE,
    LEADER_NOMINATION_CARD, // 팀장 여부 투표 카드 (AI 2명 추천 포함)
    LEADER_VOTE_CARD,
    LEADER_RESULT_CARD, // 팀장 확정 결과 카드 (아바타/이름 노출, 프로필 이동용)
    CONTEST_RECOMMEND_CARD,
    CONTEST_VOTE_CARD,
    CONTEST_VOTE_REMINDER_CARD, // 공모전 투표 마감 10분 전 리마인더 (메타데이터 없음, 버튼 라벨은 프론트가 GET .../votes의 myVoted로 결정)
    CONTEST_SHARE_CARD, // 공모전 탭에서 공유한 공모전 카드. 후보 등록은 별도 액션(+ 버튼)에서 처리
    CONTEST_RESULT_CARD, // 공모전 확정 결과 카드 (썸네일/제목/D-day 노출)
    CHATBOT_GUIDE_CARD, // "@챗봇" 활용 예시 안내 카드 ("@챗봇에게 말하기" 버튼용)
    PROGRESS_CHECK_CARD,
    SUBMISSION_CHECK_CARD
}
