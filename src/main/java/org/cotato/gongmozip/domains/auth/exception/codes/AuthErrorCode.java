package org.cotato.gongmozip.domains.auth.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 400
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_1", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_400_2", "비밀번호 재설정 인증 정보가 유효하지 않거나 만료되었습니다."),
    PASSWORD_RESET_UNAVAILABLE(HttpStatus.BAD_REQUEST, "AUTH_400_3", "이메일 로그인 계정만 비밀번호를 재설정할 수 있습니다."),
    INVALID_PASSWORD_RESET_CODE(HttpStatus.BAD_REQUEST, "AUTH_400_4", "인증코드가 올바르지 않습니다."),
    EXPIRED_PASSWORD_RESET_CODE(HttpStatus.BAD_REQUEST, "AUTH_400_5", "인증코드가 만료되었습니다."),
    PASSWORD_RESET_CODE_NOT_ISSUED(HttpStatus.BAD_REQUEST, "AUTH_400_6", "인증코드가 발급되지 않았습니다. 먼저 인증코드를 요청해 주세요."),

    // 401
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "유효하지 않은 Refresh Token입니다."),
    KAKAO_ACCOUNT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "카카오 계정 정보가 없습니다."),
    KAKAO_EMAIL_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "AUTH_401_5", "카카오 이메일 정보가 없습니다."),
    GOOGLE_EMAIL_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "AUTH_401_6", "구글 이메일 정보가 없습니다."),

    // 429
    TOO_MANY_PASSWORD_RESET_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH_429_1", "인증 시도 횟수를 초과했습니다. 새 인증코드를 요청해 주세요."),
    PASSWORD_RESET_CODE_SEND_TOO_FAST(
            HttpStatus.TOO_MANY_REQUESTS, "AUTH_429_2", "잠시 후 다시 요청해 주세요. 인증코드는 1분 후에 재요청할 수 있습니다."),

    // 500
    PASSWORD_RESET_EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_500_1", "비밀번호 재설정 인증코드 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
