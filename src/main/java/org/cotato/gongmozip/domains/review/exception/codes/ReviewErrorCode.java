package org.cotato.gongmozip.domains.review.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {

    // 400
    CANNOT_REVIEW_SELF(HttpStatus.BAD_REQUEST, "REVIEW_400_1", "본인은 리뷰할 수 없습니다."),

    // 409
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "REVIEW_409_1", "이미 이 팀원에게 리뷰를 작성했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
