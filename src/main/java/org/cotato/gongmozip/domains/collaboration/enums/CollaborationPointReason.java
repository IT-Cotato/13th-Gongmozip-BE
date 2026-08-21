package org.cotato.gongmozip.domains.collaboration.enums;

public enum CollaborationPointReason {
    LEAVE_PENALTY(-10), // 채팅방 중도 이탈
    PROGRESS_CHECK_RESPONSE(
            5), // (레거시, 2026-08-18부터 미사용) 중간점검이 팀장 응답 카드였을 때의 최초 응답 지급 사유. 과거 적립 이력 호환을 위해서만 유지 — 07-scheduler.md 참고
    PROJECT_COMPLETE_MEMBER(20), // 프로젝트 완주 (팀원)
    PROJECT_COMPLETE_LEADER(35), // 프로젝트 완주 (팀장) (2026-08-21 갱신 — PM 텍스팅 수정 반영, 기존 30)
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
