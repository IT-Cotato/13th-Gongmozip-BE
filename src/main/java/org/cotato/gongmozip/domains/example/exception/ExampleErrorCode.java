package org.cotato.gongmozip.domains.example.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExampleErrorCode implements BaseErrorCode {
    EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXAMPLE_404_1", "예시 데이터를 찾을 수 없습니다."),
    EXAMPLE_UNAUTHORIZED(HttpStatus.FORBIDDEN, "EXAMPLE_403_1", "예시 데이터에 대한 접근 권한이 없습니다."),
    EXAMPLE_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "EXAMPLE_400_1", "예시 데이터 내용이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
