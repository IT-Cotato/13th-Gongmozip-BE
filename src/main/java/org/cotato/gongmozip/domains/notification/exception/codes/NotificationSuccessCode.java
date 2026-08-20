package org.cotato.gongmozip.domains.notification.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements BaseSuccessCode {

    // 200
    NOTIFICATION_LIST_RETRIEVED(HttpStatus.OK, "NOTIFICATION_200_1", "알림 목록 조회에 성공하였습니다."),
    NOTIFICATION_UNREAD_EXISTS_RETRIEVED(HttpStatus.OK, "NOTIFICATION_200_2", "안읽은 알림 존재 여부 조회에 성공하였습니다."),
    NOTIFICATIONS_MARKED_READ(HttpStatus.OK, "NOTIFICATION_200_3", "알림을 모두 읽음 처리하였습니다."),
    PUSH_TOKEN_UNREGISTERED(HttpStatus.OK, "NOTIFICATION_200_4", "푸시 토큰이 해제되었습니다."),

    // 201
    PUSH_TOKEN_REGISTERED(HttpStatus.CREATED, "NOTIFICATION_201_1", "푸시 토큰이 등록되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
