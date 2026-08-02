package org.cotato.gongmozip.domains.review.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewSuccessCode implements BaseSuccessCode {

    // 200
    REVIEW_TARGETS_FETCHED(HttpStatus.OK, "REVIEW_200_1", "리뷰 대상 팀원 목록 조회에 성공하였습니다."),

    // 201
    REVIEW_SUBMITTED(HttpStatus.CREATED, "REVIEW_201_1", "리뷰 작성에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
