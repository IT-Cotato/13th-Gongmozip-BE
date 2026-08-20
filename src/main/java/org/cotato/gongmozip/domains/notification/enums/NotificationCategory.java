package org.cotato.gongmozip.domains.notification.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 카테고리: OTHER=기타(광고성 팝업 등), MATCHING=매칭 신청/결과, CHATROOM=챗봇이 채팅방에 남긴 카드형 안내")
public enum NotificationCategory {
    OTHER,
    MATCHING,
    CHATROOM
}
