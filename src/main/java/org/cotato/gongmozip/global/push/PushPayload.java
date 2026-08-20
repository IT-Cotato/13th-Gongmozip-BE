package org.cotato.gongmozip.global.push;

import java.util.Map;

/**
 * OS 푸시 한 건의 내용. {@code data}는 딥링크에 필요한 최소 정보만 담는다 — 채팅방(CHATROOM/일반 채팅
 * 메시지) 유래 푸시는 {@code teamId}를 담아, 프론트가 foreground일 때 "지금 그 방을 보고 있으면 표시
 * 안 함" 판단을 로컬에서 할 수 있게 한다(docs/decisions/12-frontend-notification-integration.md).
 */
public record PushPayload(String title, String body, Map<String, String> data) {

    public PushPayload {
        if (data == null) {
            data = Map.of();
        }
    }
}
