package org.cotato.gongmozip.domains.collaboration.enums;

public enum CollaborationPointReason {
    LEAVE_PENALTY(-10), // 채팅방 중도 이탈
    PROGRESS_CHECK_RESPONSE(5), // 중간점검 응답 (팀장)
    PROJECT_COMPLETE_MEMBER(20), // 프로젝트 완주 (팀원)
    PROJECT_COMPLETE_LEADER(30), // 프로젝트 완주 (팀장)
    REVIEW_WRITTEN(10), // 팀원 리뷰 작성
    MATCHING_PASS_PENALTY(-3); // 매칭 마감 이후 패스 (실제 delta는 연속 횟수에 따라 -3~-11)

    private final int defaultDelta;

    CollaborationPointReason(int defaultDelta) {
        this.defaultDelta = defaultDelta;
    }

    public int getDefaultDelta() {
        return defaultDelta;
    }
}
