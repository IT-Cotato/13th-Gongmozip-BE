package org.cotato.gongmozip.global.push;

public enum PushSendResult {
    SUCCESS,

    // 기기에서 앱 삭제/알림 권한 철회 등으로 토큰이 더 이상 유효하지 않음 — 호출부가 해당 PushToken을
    // 정리해야 한다는 신호로 쓴다.
    INVALID_TOKEN,

    // 그 외 실패(네트워크 오류, 일시적 장애 등) — 재시도하지 않는다.
    FAILURE
}
