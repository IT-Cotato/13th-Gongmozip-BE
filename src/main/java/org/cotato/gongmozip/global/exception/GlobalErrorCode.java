package org.cotato.gongmozip.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400_1", "잘못된 요청입니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_400_2", "요청 파라미터가 올바르지 않습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "COMMON_400_3", "JSON 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401_1", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403_1", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404_1", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405_1", "허용되지 않는 HTTP 메서드입니다."),
    DATA_INTEGRITY_CONFLICT(HttpStatus.CONFLICT, "COMMON_409_1", "동시에 처리된 요청과 충돌했습니다. 다시 시도해 주세요."),
    RESOURCE_LOCK_CONFLICT(HttpStatus.CONFLICT, "COMMON_409_2", "다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_1", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
