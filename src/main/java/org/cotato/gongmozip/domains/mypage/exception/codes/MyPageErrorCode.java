package org.cotato.gongmozip.domains.mypage.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyPageErrorCode implements BaseErrorCode {

    // 400
    INVALID_PAGE_INFO(HttpStatus.BAD_REQUEST, "MYPAGE_400_1", "잘못된 페이지 정보입니다."),
    PROJECT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "MYPAGE_400_2", "완료된 프로젝트가 아닙니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MYPAGE_404_1", "회원을 찾을 수 없습니다."),
    PROJECT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "MYPAGE_404_2", "해당 프로젝트 기록을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
