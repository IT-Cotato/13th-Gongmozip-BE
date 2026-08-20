package org.cotato.gongmozip.domains.notification.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    // 400
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "NOTIFICATION_400_1", "올바르지 않은 알림 카테고리입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
